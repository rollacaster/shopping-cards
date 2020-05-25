(ns tech.thomas-sojka.ingredients.core
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [clojure.walk :as w]
            [hickory.core :as html]
            [hickory.select :as select]
            [tech.thomas-sojka.ingredients.auth :refer [access-token]]))

(def trello-api "https://api.trello.com/1")
(def board-url "https://api.trello.com/1/members/me/boards")
(def creds-file (read-string (slurp ".creds.edn")))

(defn meal-line->clj [meal-line]
  (let [meal (apply str (drop 2 meal-line))]
    meal
    (if (s/starts-with? meal "[")
      (let [[_ meal-name link] (re-matches #"\[(.*)\]\((.*)\)" meal)]
        {:name meal-name :link link})
      {:name meal})))

(defn load-trello-recipes []
  (let [recipes-card-description (:desc (:body (client/get (str trello-api "/cards/" "OT6HW1Ik")
                                                           {:query-params {:key (:trello-key creds-file)
                                                                           :token (:trello-token creds-file)}
                                                            :as :json})))]
    (->> recipes-card-description
         s/split-lines
         (filter not-empty)
         (take-while #(not= % "Schnell Gerichte"))
         (map meal-line->clj))))

(defn is-link? [node] (= (:tag node) :a))

(defn transform-link [node] (first (:content node)))

(defn walk [node]
  (cond
    (is-link? node) (transform-link node)
    :else node))

(defn split-on-space [word]
  (clojure.string/split word #"\s"))

(defn trim-all [word]
  (->> word
       split-on-space
       (filter #(not (clojure.string/blank? %)))
       (clojure.string/join " ")))

(defn parse-int [s]
  (when (and s (re-find #"\d+" s))
    (Integer. (re-find #"\d+" s))))

(defn scrape-chefkoch-ingredients [link]
  (let [recipe-html (:body (client/get link))]
    (->> recipe-html
         html/parse
         html/as-hickory
         (select/select
          (select/child
           (select/class "ingredients")
           (select/tag :tbody)
           (select/tag :tr)
           (select/tag :td)))
         (map (comp :content first #(filter (complement string?) %) :content))
         (w/postwalk walk)
         (map first)
         (map #(if (string? %) (trim-all %) %))
         (partition 2)
         (map #(zipmap [:amount-desc :name] %))
         (map #(assoc % :amount (parse-int (:amount-desc %)))))))

(def drive-api-url "https://www.googleapis.com/drive/v3")

(defn oauth-token []
  (access-token {:client-id (:drive-client-id creds-file)
                 :client-secret (:drive-client-secret creds-file)
                 :redirect-uri "http://localhost:8080"
                 :scope ["https://www.googleapis.com/auth/drive"
                         "https://www.googleapis.com/auth/drive.file"]}))

(defn scrape-gdrive-ingredient [ingredient-line]
  (let [ingredient (s/split (apply str (drop 2 ingredient-line)) #" ")]
    (if (and (> (count ingredient) 1) (parse-int (first ingredient)))
      {:amount-desc (first ingredient) :name (last ingredient) :amount (parse-int (first ingredient))}
      {:amount-desc nil :name (s/join " "ingredient) :amount nil})))

(defn scrape-gdrive-ingredients [link]
  (let [recipe-id ((s/split link #"/") 5)
        recipe-text (:body (client/get (str drive-api-url "/files/" recipe-id "/export")
                                       {:oauth-token (oauth-token) :query-params {:mimeType "text/plain"}}))]
    (map scrape-gdrive-ingredient (take-while #(s/starts-with? % "*") (drop 1 (s/split-lines recipe-text))))))

(defn scrape-eat-this-span [class spans]
  (first (:content (some #(when (= (get-in % [:attrs :class]) class) %) spans))))

(defn scrape-eath-this-ingredient [ingredient-li]
  (let [spans
        (->> ingredient-li
             :content
             (filter #(not= % " ")))
        amount (parse-int (scrape-eat-this-span "wprm-recipe-ingredient-amount" spans))
        unit (scrape-eat-this-span "wprm-recipe-ingredient-unit" spans)]
    {:amount amount
     :amount-desc (str (or (and amount unit (str amount " " unit))
                        amount
                        unit
                        nil))
     :name (scrape-eat-this-span "wprm-recipe-ingredient-name" spans)
     :unit unit}))

(defn scrape-eat-this-ingredients [link]
  (let [recipe-html (:body (client/get link))]
    (->> recipe-html
         html/parse
         html/as-hickory
         (select/select
          (select/child
           (select/class "wprm-recipe-ingredient")))
         (w/postwalk walk)
         (map scrape-eath-this-ingredient))))

(defn add-ingredients [recipes]
  (->> recipes
       (map #(cond (and (:link %) (s/includes? (:link %) "chefkoch") (not (:ingredients %)))
                   (assoc % :ingredients (scrape-chefkoch-ingredients (:link %)))
                   (and (:link %) (s/includes? (:link %) "docs.google") (not (:ingredients %)))
                   (assoc % :ingredients (scrape-gdrive-ingredients (:link %)))
                   (and (:link %) (s/includes? (:link %) "eat-this") (not (:ingredients %)))
                   (assoc % :ingredients (scrape-eat-this-ingredients (:link %)))
                   :else %))))

(defn load-recipes []
  (read-string (slurp "resources/recipes.edn")))

(defn write-edn [path data]
  (spit path (prn-str data)))

(defn write-recipes [recipes]
  (write-edn "resources/recipes.edn" (vec recipes)))

(comment
  (->> (load-trello-recipes)
       add-ingredients
       write-recipes)
  (load-recipes))


