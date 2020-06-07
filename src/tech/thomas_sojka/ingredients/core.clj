(ns tech.thomas-sojka.ingredients.core
  (:require [clj-http.client :as client]
            clojure.java.io
            clojure.pprint
            [clojure.string :as s]
            [clojure.walk :as w]
            [hickory.core :as html]
            [hickory.select :as select]
            [tech.thomas-sojka.ingredients.auth :refer [access-token]]
            [tick.core :refer [now]]))

(def trello-api "https://api.trello.com/1")
(def board-url "https://api.trello.com/1/members/me/boards")
(def creds-file (read-string (slurp ".creds.edn")))
(def search-engine-cx "005510767845232759155:zdkkvfzersx")

(defn meal-line->clj [meal-line]
  (let [meal (apply str (drop 2 meal-line))]
    meal
    (if (s/starts-with? meal "[")
      (let [[_ meal-name link] (re-matches #"\[(.*)\]\((.*)\)" meal)]
        {:name meal-name :link link})
      {:name meal})))

(defn load-trello-recipes []
  (let [recipes-card-description
        (:desc (:body (client/get (str trello-api "/cards/" "OT6HW1Ik")
                                  {:query-params
                                   {:key (:trello-key creds-file)
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

(defn add-ingredients [recipes]
  (->> recipes
       (map #(cond (and (:link %)
                        (s/includes? (:link %) "chefkoch")
                        (not (:ingredients %)))
                   (assoc %
                          :ingredients (scrape-chefkoch-ingredients (:link %)))
                   (and (:link %)
                        (s/includes? (:link %) "docs.google")
                        (not (:ingredients %)))
                   (assoc %
                          :ingredients (scrape-gdrive-ingredients (:link %)))
                   (and (:link %)
                        (s/includes? (:link %) "eat-this")
                        (not (:ingredients %)))
                   (assoc %
                          :ingredients (scrape-eat-this-ingredients (:link %)))
                   :else %))))

(defn find-recipe-image [recipe-name]
  (-> "https://customsearch.googleapis.com/customsearch/v1"
      (client/get
       {:query-params {:q (s/replace recipe-name " " "+")
                       :num 1
                       :start 1
                       :imgSize "medium"
                       :searchType "image"
                       :cx search-engine-cx
                       :key (:google-key creds-file)}
        :as :json :throw-entire-message? true})
      :body :items first :link))

(defn load-edn [path] (read-string (slurp (str "resources/" path))))
(defn load-recipes [] (load-edn "recipes.edn"))
(defn load-ingredients [] (load-edn "ingredients.edn"))
(defn load-cooked-with [] (load-edn "cooked-with.edn"))

(defn write-edn [path data]
  (clojure.pprint/pprint data (clojure.java.io/writer (str "resources/" path))))

(comment
  (defn uuid [] (str (java.util.UUID/randomUUID)))

  (defn vec->map [key-name vec]
    (zipmap (map key-name vec) vec))

  (defn merge-recipe-lists [a b]
    (map (fn [[_ val]] val) (merge-with merge
                                       (vec->map :name a)
                                       (vec->map :name b))))

  (defn write-recipes []
    (->> (merge-recipe-lists (load-trello-recipes) (vals (load-recipes)))
         add-ingredients
         (map #(if (:image %)
                 %
                 (assoc % :image (find-recipe-image (:name %)))))
         vec))

  (defn normalize-ingredients [duplicated-ingredients ingredients]
    (map (fn [{:keys [name] :as ingredient}]
           (assoc ingredient :name
                  (or
                   (some (fn [[ingredient-group-name duplicated-name]]
                           (when (or (= name ingredient-group-name)
                                     (contains? duplicated-name name))
                             ingredient-group-name))
                         duplicated-ingredients)
                   name)))
         ingredients))

  (defn ingredient-list [recipes]
    (let [duplicated-ingredients
          (load-edn "duplicated-ingredients.edn")]
      (->> recipes
           (map :ingredients)
           flatten
           (normalize-ingredients duplicated-ingredients)
           (group-by :name)))))

(defn ingredient-text [ingredients]
  (str (count ingredients)
       " " (:name (first ingredients))
       " (" (s/join ", " (map :amount-desc ingredients)) ")"))

(def penny-order
  ["Gemüse"
   "Gewürze"
   "Tiefkühl"
   "Brot & Co"
   "Müsli & Co"
   "Konserven"
   "Beilage"
   "Backen"
   "Fleisch"
   "Wursttheke"
   "Milch & Co"
   "Käse & Co"
   "Eier"
   "Getränke"])

(defn ingredients-for-recipes [selected-recipe-ids]
  (->> (load-cooked-with)
       (filter #(contains? selected-recipe-ids (:recipe-id %)))
       (map (fn [{:keys [ingredient-id amount-desc amount]}]
              (merge {:amount-desc amount-desc
                      :amount amount}
                     (some #(when (= (:id %) ingredient-id) %) (load-ingredients)))))
       (remove #(= (:category %) "Gewürze"))
       (remove #(= (:category %) "Backen"))
       (group-by :category)
       (sort-by :category (fn [a b] (< (.indexOf penny-order a) (.indexOf penny-order b))))
       (map second)
       flatten
       (group-by :id)
       (map (fn [[id ingredients]] (vector id (ingredient-text ingredients))))))

(defn category-ingredients->str [{:keys [ingredients]}]
  (->> ingredients
       (map #(str "- " %))
       (s/join "\n")))

(def klaka-board-id "48aas65T")

(defn load-trello-lists [board-id]
  (client/get (str trello-api "/boards/" board-id "/lists")
              {:query-params
               {:key (:trello-key creds-file)
                :token (:trello-token creds-file)}
               :as :json
               :throw-entire-message? true}))

(defn create-trello-shopping-card [list-id]
  (:body (client/post (str trello-api "/cards/")
                      {:query-params
                       {:key (:trello-key creds-file)
                        :token (:trello-token creds-file)
                        :name (str "Einkaufen " (apply str (take 10 (str (now)))))
                        :idList list-id}
                       :as :json
                       :throw-entire-message? true})))

(defn create-trello-checklist [card-id]
  (:body
   (client/post (str trello-api "/checklists")
                {:query-params
                 {:key (:trello-key creds-file)
                  :token (:trello-token creds-file)
                  :idCard card-id}
                 :as :json
                 :throw-entire-message? true})))

(defn create-trell-checklist-item [checklist-id item]
  (:body (client/post (str trello-api (str "/checklists/" checklist-id "/checkItems"))
                      {:query-params
                       {:key (:trello-key creds-file)
                        :token (:trello-token creds-file)
                        :name item}
                       :as :json
                       :throw-entire-message? true})))

(defn create-klaka-shopping-card [ingredients]
  (let [list-id (:id (first (:body (load-trello-lists "G2Ysmygl")))) ;; TODO Replace with Klaka-Id
        card-id (:id (create-trello-shopping-card list-id))
        checklist-id (:id (create-trello-checklist card-id))]
    (map #(create-trell-checklist-item checklist-id %) ingredients)))





