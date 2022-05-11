(ns tech.thomas-sojka.shopping-cards.scrape
  (:require
   [cheshire.core :refer [parse-string]]
   [clj-http.client :as client]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :refer [sh]]
   [clojure.string :as s]
   [clojure.walk :as w]
   [datomic.client.api :as d]
   [hickory.core :as html]
   [hickory.select :as select]
   [tech.thomas-sojka.shopping-cards.auth :refer [access-token creds-file]]
   [tech.thomas-sojka.shopping-cards.queries :as queries]))

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
  (when s
    (cond (re-find #"[\d]+" s) (edn/read-string (re-find #"[\d/]+" s))
          (re-find #"¼" s) 0.25)))


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

(def units ["g" "Esslöffel" "ml" "Handvoll" "Teelöffel" "EL" "TL" "Dose" "Zehen" "Tasse" "ein paar"])

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

(defn scrape-eatsmarter [recipe-hickory]
  (->> recipe-hickory
       (select/select (select/tag "dd"))
       (map (fn [node]
              (let [contents ((comp :content first :content first :content) node)
                    [unit-node ingredient-node] contents
                    amount (some-> (first (:content unit-node)) s/trim)
                    unit ((comp first :content) (second (:content unit-node)))
                    name ((comp first :content first) (select/select (select/tag :a) ingredient-node))]
                {:amount amount
                 :unit unit
                 :amount-desc (str amount unit " " name)
                 :name name})))))

(defn scrape-cookidoo [recipe-hickory]
  (->> recipe-hickory
       (select/select (select/id "ingredients"))
       first
       :content
       (drop 3)
       (mapcat :content)
       (drop 3)
       (mapcat :content)
       (filter map?)
       (map (fn [node]
              (let [ing-text (-> node
                                 :content
                                 first
                                 (s/trim )
                                 (s/replace #"\s+" " "))
                    unit (some #(when (.contains ing-text (str " " % " ")) %) units)
                    amount (parse-int ing-text)]
                {:name (s/trim (s/replace ing-text (str amount " " unit) ""))
                 :amount-desc (s/trim (str amount " " unit))
                 :unit unit
                 :amount amount})))))


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
        (s/includes? link "eatsmarter")
        (scrape-eatsmarter recipe-hickory)
        (s/includes? link "cookidoo")
        (scrape-cookidoo recipe-hickory)))

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

(defn load-edn [path] (read-string (slurp (io/resource path))))

(defn ingredient-name [name]
  (some (fn [[ingredient-group-name duplicated-name]]
          (when (or (= name ingredient-group-name)
                    (contains? duplicated-name name))
            ingredient-group-name))
        (load-edn "duplicated-ingredients.edn")))

(defn throw-for-unknown-ingredients [ingredients]
  (let [unknown-ingredients (remove :id ingredients)]
    (if (seq unknown-ingredients)
      (throw (ex-info "Unkown ingredients found!"
                      {:unknown-ingredients (map :name unknown-ingredients)}))
      ingredients)))

(defn dedup-ingredients [all-ingredients ingredients-to-dedup]
  (->> ingredients-to-dedup
       (mapv (fn [{:keys [name] :as ingredient}]
               (let [ingredient-name (or (ingredient-name name) name)]
                 (assoc ingredient
                        :name ingredient-name
                        :id (some #(when (= (:ingredient/name %) ingredient-name) (:ingredient/id %)) (map first all-ingredients))))))
       throw-for-unknown-ingredients))

(defmulti recipe-name (fn [link _] (cond
                                  (s/includes? link "kptncook") :kptncook
                                  (s/includes? link "meinestube") :meinestube
                                  :else :chefkoch)))

(defmethod recipe-name :cookidoo [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/class "recipe-card__title"))
       first
       :content
       first
       s/trim))

(defmethod recipe-name :kptncook [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/tag "title"))
       first
       :content
       first
       s/trim))

(defmethod recipe-name :eatsmarter [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/tag "h1"))
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
  (let [ingredient (s/split (apply str (drop 2 ingredient-line)) #" ")
        unit (some (fn [unit] (when (s/includes? ingredient-line (str " " unit " ")) unit)) units)]
    (if (and (> (count ingredient) 1) (parse-int (first ingredient)))
      {:amount-desc (first ingredient)
       :name (s/replace (s/join " " (rest ingredient))
                        (re-pattern (str unit " ")) "")
       :amount (parse-int (first ingredient))
       :unit unit}
      {:amount-desc nil
       :name (s/replace (s/join " "ingredient)
                        (re-pattern (str " " unit " ")) "")
       :amount nil})))



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
              (take-while #(or (s/starts-with? % "*")
                               (s/starts-with? % "•")))))))

(defn fetch-gdrive-title [link]
  (let [recipe-id ((s/split link #"/") 5)
        doc
        (:body (client/get (str drive-api-url "/files/" recipe-id)
                           {:oauth-token (oauth-token)
                            :query-params {:mimeType "text/plain"}}))]
    (:name (parse-string doc true))))


(defn as-hickory [link]
  (->> (:body (client/get link {:headers {"Accept-Language" "de-DE,de;q=0.9,en-DE;q=0.8,en;q=0.7,en-US;q=0.6"}}))
       html/parse
       html/as-hickory))

(defn scrape-recipe [conn {:keys [link type name image] :or {type :recipe-type/normal}}]
  (if (s/includes? link "kptncook")
    (let [{:keys [name ingredients]} (parse-string (:out (sh "node" "src/js/scrape.js" link)) true)]
      (-> {:name name
           :ingredients ingredients
           :inactive false
           :type type
           :image (find-image name)
           :link link}
          (update :ingredients (partial dedup-ingredients (d/q queries/load-ingredients conn)))))
    (let [recipe-hickory (as-hickory link)
          name (or name
                   (when (s/includes? link "docs.google")
                         (fetch-gdrive-title link))
                   (recipe-name link recipe-hickory))]
      (conj (->> recipe-hickory
                (add-ingredients link)
                (if (s/includes? link "docs.google") (fetch-gdrive-ingredients link))
                (dedup-ingredients (d/q queries/load-ingredients (d/db conn)))
                (map (fn [{:keys [amount-desc unit id]}]
                       (cond->
                           #:cooked-with{:id (str (random-uuid))
                                         :amount-desc amount-desc
                                         :ingredient [:ingredient/id id]
                                         :recipe name}
                         unit (assoc :cooked-with/unit unit)))))
           (->
            {:db/id name
             :recipe/id (str (random-uuid))
             :recipe/name name
             :recipe/link link
             :recipe/type type}
            (assoc :recipe/image (or image (find-image name))))))))

(comment
  (def client (d/client {:server-type :dev-local :system "dev"}))
  (def conn (d/connect client {:db-name "shopping-cards"}))
  (d/transact
   conn
   {:tx-data
    [{:ingredient/category #:db{:ident :ingredient-category/gemürze},
      :ingredient/id (str (random-uuid)),
      :ingredient/name "Lorbeerblatt"}]})
  (d/transact
   conn
   {:tx-data
    (scrape-recipe conn {:link "https://cookidoo.de/recipes/recipe/de/r673004"
                         :image "https://assets.tmecosys.com/image/upload/t_web667x528/img/recipe/ras/Assets/c277dd26-4973-4802-8e98-0281491c9ac3/Derivates/ea5f1a84-a9bb-40ed-98c3-598ded6cd11f.jpg"})}))
