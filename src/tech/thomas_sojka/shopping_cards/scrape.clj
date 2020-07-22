(ns tech.thomas-sojka.shopping-cards.scrape
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [clojure.walk :as w]
            [hickory.core :as html]
            [hickory.select :as select]
            [tech.thomas-sojka.shopping-cards.auth :refer [access-token creds-file]]
            [tech.thomas-sojka.shopping-cards.db :as db]))

(def drive-api-url "https://www.googleapis.com/drive/v3")
(def search-engine-cx "005510767845232759155:zdkkvfzersx")

(defn oauth-token []
  (access-token {:client-id (:drive-client-id creds-file)
                 :client-secret (:drive-client-secret creds-file)
                 :redirect-uri "http://localhost:8080"
                 :scope ["https://www.googleapis.com/auth/drive"
                         "https://www.googleapis.com/auth/drive.file"]}))

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

(defn scrape-gdrive-ingredient [ingredient-line]
  (let [ingredient (s/split (apply str (drop 2 ingredient-line)) #" ")]
    (if (and (> (count ingredient) 1) (parse-int (first ingredient)))
      {:amount-desc (first ingredient)
       :name (last ingredient)
       :amount (parse-int (first ingredient))}
      {:amount-desc nil :name (s/join " "ingredient) :amount nil})))

(defn scrape-gdrive-ingredients [link]
  (let [recipe-id ((s/split link #"/") 5)
        recipe-text
        (:body (client/get (str drive-api-url "/files/" recipe-id "/export")
                           {:oauth-token (oauth-token)
                            :query-params {:mimeType "text/plain"}}))]
    (map scrape-gdrive-ingredient
         (->> recipe-text
              s/split-lines
              (drop 1)
              (take-while #(s/starts-with? % "*"))))))

(defn scrape-eat-this-span [class spans]
  (first (:content (some
                    #(when (= (get-in % [:attrs :class]) class) %)
                    spans))))

(defn scrape-eath-this-ingredient [ingredient-li]
  (let [spans
        (->> ingredient-li
             :content
             (filter #(not= % " ")))
        amount (parse-int (scrape-eat-this-span
                           "wprm-recipe-ingredient-amount" spans))
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

(defn scrape-weightwatchers [link]
  (let [recipe-html (:body (client/get link))]
    (let [ingredients (->> recipe-html
                           html/parse
                           html/as-hickory
                           (select/select
                            (select/child
                             (select/class "VerticalList_listTwoColumn__a4AGp")))
                           (map :content)
                           first
                           (map (comp  :content first :content first :content first :content)))
          names (map (comp s/trim first :content first) ingredients)
          amounts (map (comp parse-int first :content first :content second) ingredients)
          amount-descs (map (comp first :content second :content second) ingredients)]
      (map (fn [name amount amount-desc]
             {:name name
              :amount amount
              :amount-desc (str amount amount-desc)
              :unit (s/trim amount-desc)})
           names amounts amount-descs))))

(defn add-ingredients [recipe]
  (cond (and (:link recipe)
             (s/includes? (:link recipe) "chefkoch")
             (not (:ingredients recipe)))
        (assoc recipe
               :ingredients (scrape-chefkoch-ingredients (:link recipe)))
        (and (:link recipe)
             (s/includes? (:link recipe) "docs.google")
             (not (:ingredients recipe)))
        (assoc recipe
               :ingredients (scrape-gdrive-ingredients (:link recipe)))
        (and (:link recipe)
             (s/includes? (:link recipe) "eat-this")
             (not (:ingredients recipe)))
        (assoc recipe
               :ingredients (scrape-eat-this-ingredients (:link recipe)))
        (and (:link recipe)
             (s/includes? (:link recipe) "weightwatchers")
             (not (:ingredients recipe)))
        (assoc recipe
               :ingredients (scrape-weightwatchers (:link recipe)))
        :else recipe))

(defn find-image [recipe]
  (assoc recipe :image
         (-> "https://customsearch.googleapis.com/customsearch/v1"
             (client/get
              {:query-params {:q (s/replace (:name recipe) " " "+")
                              :num 1
                              :start 1
                              :imgSize "medium"
                              :searchType "image"
                              :cx search-engine-cx
                              :key (:google-key creds-file)}
               :as :json :throw-entire-message? true})
             :body :items first :link)))

(defn dedup-ingredients [recipe]
  (update recipe :ingredients
          (fn [ingredients]
            (map (fn [{:keys [name] :as ingredient}]
                   (let [ingredient-name (or
                                          (some (fn [[ingredient-group-name duplicated-name]]
                                                  (when (or (= name ingredient-group-name)
                                                            (contains? duplicated-name name))
                                                    ingredient-group-name))
                                                (db/load-edn "duplicated-ingredients.edn"))
                                          name)]
                     (assoc ingredient
                            :name ingredient-name
                            :id (some #(when (= (:name %) ingredient-name) (:id %)) (db/load-edn "ingredients.edn")))))
                 ingredients))))


(defn add-chefkoch-recipe [{:keys [link type]}]
  (->>
   (let [recipe-html (:body (client/get link))
         recipe-hickory (->> recipe-html html/parse html/as-hickory)]
     {:name (->> recipe-hickory
                  (select/select (select/child (select/tag "h1")))
                  first :content first)
      :link link
      :type type
      :inactive false})
   find-image
   add-ingredients
   dedup-ingredients))

;; TODO Handle new ingredients before adding recipe
(defn new-ingredients [new-recipes]
  (->> new-recipes
       (map :ingredients)
       flatten
       (remove :id)))

(defn uuid [] (str (java.util.UUID/randomUUID)))


(comment
  )
