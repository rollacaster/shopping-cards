(ns tech.thomas-sojka.shopping-cards.scrape
  (:require [clj-http.client :as client]
            [clojure.edn :as edn]
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
    (edn/read-string (re-find #"[\d/]+" s))))


(defn scrape-chefkoch-ingredients [recipe-hickory]
  (->> recipe-hickory
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
       (map #(assoc % :amount (parse-int (:amount-desc %))))))

(def units ["g" "Esslöffel" "ml" "Handvoll" "Teelöffel" "EL"])

(defn scrape-springlane-ingredient [ingredient-line]
  (let [unit (some (fn [unit] (when (s/includes? ingredient-line (str " " unit " ")) unit)) units)
        amount (parse-int ingredient-line)]
    {:amount-desc ingredient-line
     :amount amount
     :name (-> ingredient-line
               (s/replace (re-pattern (str amount)) "")
               (s/replace (re-pattern (str " " unit " ")) "")
               s/trim)
     :unit unit}))

(defn scrape-springlane [recipe-hickory]
  (->> recipe-hickory
       (select/select
        (select/child
         (select/class "recipe-ingredients-list")))
       first :content
       (remove string?)
       (map (comp s/trim first :content))
       (map scrape-springlane-ingredient)))

(defn scrape-kptcook [recipe-hickory]
  (let [scrape-class (fn [class]
                       (->> recipe-hickory
                            (select/select
                             (select/child
                              (select/class class)))))
        measures (->> (scrape-class "kptn-ingredient-measure")
                      (map (comp s/trim first :content))
                      (map (fn [amount-desc] {:amount-desc amount-desc
                                             :amount (parse-int amount-desc)
                                             :unit (let [last-part (last (s/split amount-desc #" "))]
                                                     (when-not (parse-int last-part) last-part))})))
        ingredients (->> (scrape-class "kptn-ingredient")
                         (map (comp s/trim first :content))
                         (map (fn [name] {:name name})))]
    (concat
     (map
      merge
      ingredients
      measures)
     (drop
      (count measures)
      ingredients))))

(defn scrape-eat-this-span [class spans]
  (first (:content (some
                    #(when (= (get-in % [:attrs :class]) class) %)
                    spans))))

(defn scrape-eat-this-ingredient [ingredient-li]
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

(defn scrape-eat-this-ingredients [recipe-hickory]
  (let [wprm-ingredients (->> recipe-hickory
                          (select/select
                           (select/child
                            (select/class "wprm-recipe-ingredient")))
                          (w/postwalk walk)
                          (map scrape-eat-this-ingredient))]
    (if (> (count wprm-ingredients) 0)
      wprm-ingredients
      (->> recipe-hickory
           (select/select
            (select/child
             (select/class "Zutaten")
             (select/tag "ul")))
           first
           :content
           (map (comp first :content))
           (map scrape-springlane-ingredient)))))

(defn scrape-weightwatchers [recipe-hickory]
  (let [ingredients (->> recipe-hickory
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
         names amounts amount-descs)))

(defn add-ingredients [link recipe-hickory]
  (cond (s/includes? link "chefkoch")
        (scrape-chefkoch-ingredients recipe-hickory)
        (or (s/includes? link "eat-this")
            (s/includes? link "thomassixt")
            (s/includes? link "kochkarussell")
            (s/includes? link "meinestube"))
        (scrape-eat-this-ingredients recipe-hickory)
        (s/includes? link "weightwatchers")
        (scrape-weightwatchers recipe-hickory)
        (s/includes? link "springlane")
        (scrape-springlane recipe-hickory)
        (s/includes? link "kptncook")
        (scrape-kptcook recipe-hickory)))

(defn find-image [recipe-name]
  (-> (client/get "https://customsearch.googleapis.com/customsearch/v1"
                  {:query-params {:q (s/replace recipe-name " " "+")
                                  :num 1
                                  :start 1
                                  :imgSize "medium"
                                  :searchType "image"
                                  :cx search-engine-cx
                                  :key (:google-key creds-file)}
                   :as :json :throw-entire-message? true})
      :body :items first :link))

(defn dedup-ingredients [ingredients]
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
       ingredients))


(defmulti recipe-name (fn [link _] (cond
                                  (s/includes? link "kptncook") :kptncook
                                  (s/includes? link "meinestube") :meinestube
                                  :else :chefkoch)))
(defmethod recipe-name :kptncook [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/child (select/class "kptn-recipetitle")))
       first
       :content
       first
       s/trim))

(defmethod recipe-name :chefkoch [_ recipe-hickory]
  (let [recipe-name (->> recipe-hickory
                         (select/select (select/child (select/tag "h1")))
                         first :content first)]
    (if (string? recipe-name) recipe-name (-> recipe-name :content first))))

(defn gdrive-ingredient [ingredient-line]
  (let [ingredient (s/split (apply str (drop 2 ingredient-line)) #" ")]
    (if (and (> (count ingredient) 1) (parse-int (first ingredient)))
      {:amount-desc (first ingredient)
       :name (s/join " " (rest ingredient))
       :amount (parse-int (first ingredient))}
      {:amount-desc nil :name (s/join " "ingredient) :amount nil})))


(defn fetch-gdrive-ingredients [link]
  (let [recipe-id ((s/split link #"/") 5)
        recipe-text
        (:body (client/get (str drive-api-url "/files/" recipe-id "/export")
                           {:oauth-token (oauth-token)
                            :query-params {:mimeType "text/plain"}}))]
    (map gdrive-ingredient
         (->> recipe-text
              s/split-lines
              (drop 1)
              (take-while #(s/starts-with? % "*"))))))

(defn scrape-recipe [{:keys [link type name] :or {type "NORMAL"}}]
  (let [recipe-hickory (->> (:body (client/get link {:headers {"Accept-Language" "de-DE,de;q=0.9,en-DE;q=0.8,en;q=0.7,en-US;q=0.6"}})) html/parse html/as-hickory)
        name (or name (recipe-name link recipe-hickory))]
    (->
     {:name name
      :link link
      :type type
      :inactive false}
     (assoc :image (find-image name))
     (assoc :ingredients
            (if (s/includes? link "docs.google")
              (fetch-gdrive-ingredients link)
              (add-ingredients link recipe-hickory)))
     (update :ingredients dedup-ingredients))))

;; TODO Handle new ingredients before adding recipe
(defn new-ingredients [new-recipes]
  (->> new-recipes
       (map :ingredients)
       flatten
       (remove :id)))

(defn uuid [] (str (java.util.UUID/randomUUID)))


(comment
  (scrape-recipe {:name "Vegetarisches Gulasch á la Margarete"
                  :link "https://docs.google.com/document/d/1SDgNCPGMwaKdHEmF1yTOHndItLqkIdiLN879BhMlaZE/edit"})


  (scrape-recipe {:link "https://www.meinestube.de/zucchini-frischkaese/"}
                 )
  )
