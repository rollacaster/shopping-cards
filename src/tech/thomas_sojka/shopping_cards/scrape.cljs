(ns tech.thomas-sojka.shopping-cards.scrape
  (:require ["firebase/app" :as firebase]
            ["firebase/auth" :as auth]
            ["firebase/firestore" :as firestore]
            [cljs-bean.core :refer [->clj ->js]]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.walk :as w]
            [fs :as fs]
            [hickory.core :as html]
            [hickory.select :as select]
            [tech.thomas-sojka.shopping-cards.cooked-with :as cooked-with]
            [tech.thomas-sojka.shopping-cards.specs]
            [tech.thomas-sojka.shopping-cards.google-auth :refer [access-token creds-file]]))

(def drive-api-url "https://www.googleapis.com/drive/v3")
(def firebaseConfig
  #js {:apiKey "AIzaSyAXNnWku5rgm33z6FtDoJu-IEEf1Y7RC1I"
       :authDomain "shopping-cards-e24af.firebaseapp.com"
       :projectId "shopping-cards-e24af"
       :storageBucket "shopping-cards-e24af.appspot.com"
       :messagingSenderId "235874496901"
       :appId "1:235874496901:web:dd7f231adee24e4b8f54ec"})

(defn oauth-token []
  (access-token {:client-id (:drive-client-id creds-file)
                 :client-secret (:drive-client-secret creds-file)
                 :redirect-uri "http://localhost:8080"
                 :scope ["https://www.googleapis.com/auth/drive"
                         "https://www.googleapis.com/auth/drive.file"]}))

(def app (firebase/initializeApp firebaseConfig))
(def auth (auth/getAuth firebase/app))
(def db (.getFirestore firestore firebase/app))
(set! js/DOMParser (.-DOMParser (js/require "xmldom")))
(def ingredients (atom []))

(defn load-ingredients []
  (if (empty? @ingredients)
    (-> (auth/signInWithEmailAndPassword auth "thsojka@web.de" "test123")
        (.then (fn []
                 (-> (firestore/getDocs (firestore/collection db "ingredients"))
                     (.then (fn [snapshot]
                              (let [data (volatile! [])]
                                (.forEach snapshot (fn [doc] (vswap! data conj (-> doc .data ->clj))))
                                (reset! ingredients @data))))))))
    (js/Promise.resolve @ingredients)))

(defn as-hickory [link]
  (-> (js/fetch link
                (clj->js {:headers {"Accept-Language" "de-DE,de;q=0.9,en-DE;q=0.8,en;q=0.7,en-US;q=0.6"}}))
      (.then (fn [res] (.text res)))
      (.then (fn [text] (html/as-hickory (html/parse text))))))

(defn- recipe-source [link]
  (cond
    (str/includes? link "kptncook") :kptncook
    (str/includes? link "meinestube") :meinestube
    (str/includes? link "kitchenstories") :kitchenstories
    (str/includes? link "docs.google") :google-docs
    (some #(str/includes? link %) ["eat-this" "thomassixt" "kochkarussell" "meinestube" "cuisini" "gaumenfreundin"]) :wprm
    :else :chefkoch))

(defmulti recipe-name (fn [link _] (recipe-source link)))

(defmethod recipe-name :google-docs [_ doc]
  (:name doc))

(defmethod recipe-name :kitchenstories [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/class "recipe-title"))
       first
       :content
       first
       str/trim))

(defmethod recipe-name :cookidoo [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/class "recipe-card__title"))
       first
       :content
       first
       str/trim))

(defmethod recipe-name :kptncook [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/tag "title"))
       first
       :content
       first
       str/trim))

(defmethod recipe-name :eatsmarter [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/tag "h1"))
       first
       :content
       first
       str/trim))

(defmethod recipe-name :wprm [_ recipe-hickory]
  (def recipe-hickory recipe-hickory)
  (->> recipe-hickory
       (select/select (select/class "wprm-recipe-name"))
       first
       :content
       first
       str/trim))

(defmulti recipe-image (fn [link _] (recipe-source link)))

(defmethod recipe-image :wprm [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/class "wprm-recipe-image"))
       first
       :content
       first
       :attrs
       :data-src))

(defmethod recipe-image :chefkoch [_ recipe-hickory]
  (->> recipe-hickory
       (select/select (select/class "recipe-image-carousel-slide"))
       first
       :content
       second
       :content
       second
       :attrs
       :src))

(defmethod recipe-name :chefkoch [_ recipe-hickory]
  (let [recipe-name (->> recipe-hickory
                         (select/select (select/child (select/tag "h1")))
                         first :content first)]
    (if (string? recipe-name) recipe-name (-> recipe-name :content first))))

(defmulti recipe-ingredients (fn [link _] (recipe-source link)))

(defn is-link? [node] (= (:tag node) :a))

(defn transform-link [node] (first (:content node)))

(defn walk [node]
  (cond
    (is-link? node) (transform-link node)
    :else node))

(defn wprm-span [class spans]
  (first (:content (some
                    #(when (= (get-in % [:attrs :class]) class) %)
                    spans))))

(defn parse-int [s]
  (when s
    (cond (re-find #"[\d]+" s) (parse-double (re-find #"[\d/]+" s))
          (re-find #"¼" s) 0.25)))

(defn wprm-ingredient [ingredient-li]
  (let [spans
        (->> ingredient-li
             :content
             (filter #(not= % " ")))
        amount (parse-int (wprm-span "wprm-recipe-ingredient-amount" spans))
        unit (wprm-span "wprm-recipe-ingredient-unit" spans)]
    (cond-> {:ingredient/name (wprm-span "wprm-recipe-ingredient-name" spans)
             :cooked-with/amount-desc (str (or (and amount unit (str amount " " unit))
                                     amount unit nil))}
        amount (assoc :cooked-with/amount amount)
        unit (assoc :cooked-with/unit unit))))

(defmethod recipe-ingredients :wprm [_ recipe-hickory]
  (let [wprm-ingredients (->> recipe-hickory
                              (select/select
                               (select/child
                                (select/class "wprm-recipe-ingredient")))
                              (w/postwalk walk)
                              (mapv wprm-ingredient))]
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
           (map wprm-ingredient)))))

(defn split-on-space [word]
  (clojure.string/split word #"\s"))

(defn trim-all [word]
  (->> word
       split-on-space
       (filter #(not (clojure.string/blank? %)))
       (clojure.string/join " ")))

(defmethod recipe-ingredients :chefkoch [_ recipe-hickory]
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
       (map #(zipmap [:cooked-with/amount-desc :ingredient/name] %))
       (map #(assoc % :cooked-with/amount (parse-int (:cooked-with/amount-desc %))))))

;; TODO Load units from db
(def units ["g" "Esslöffel" "ml" "Handvoll" "Teelöffel" "EL" "TL" "Dose" "Zehen" "Tasse" "ein paar"])

(defn gdrive-ingredient [ingredient-line]
  (let [ingredient (str/split (apply str (drop 2 ingredient-line)) #" ")
        unit (some (fn [unit] (when (str/includes? ingredient-line (str " " unit " ")) unit)) units)]
    (if (and (> (count ingredient) 1) (parse-int (first ingredient)))
      {:cooked-with/amount-desc (first ingredient)
       :ingredient/name (str/replace (str/join " " (rest ingredient))
                        (re-pattern (str unit " ")) "")
       :cooked-with/amount (parse-int (first ingredient))
       :cooked-with/unit unit}
      {:cooked-with/amount-desc nil
       :ingredient/name (str/replace (str/join " "ingredient)
                        (re-pattern (str " " unit " ")) "")
       :cooked-with/amount nil})))

(defmethod recipe-ingredients :google-docs [_ {:keys [content]}]
  (map gdrive-ingredient
       (->> content
            str/split-lines
            (drop 1)
            (take-while #(or (str/starts-with? % "*")
                             (str/starts-with? % "•"))))))

(defn ingredient-name [name]
  (some (fn [[ingredient-group-name duplicated-name]]
          (when (or (= name ingredient-group-name)
                    (contains? duplicated-name name))
            ingredient-group-name))
        (edn/read-string (.toString (fs/readFileSync "resources/duplicated-ingredients.edn")))))

(defn throw-for-unknown-ingredients [ingredients]
  (let [unknown-ingredients (remove :cooked-with/ingredient ingredients)]
    (if (seq unknown-ingredients)
      (throw (ex-info (str "Unkown ingredients found! " (str/join ", " (map :ingredient/name unknown-ingredients)))
                      {:unknown-ingredients (map :ingredient/name unknown-ingredients)}))
      ingredients)))

(def unused-ingredients #{"Salbei" "Lavendelblüten"})

(defn dedup-ingredients [all-ingredients ingredients-to-dedup]
  (->> ingredients-to-dedup
       (mapv (fn [{:keys [ingredient/name] :as ingredient}]
               (let [ingredient-name (or (ingredient-name name) name)]
                 (assoc ingredient
                        :ingredient/name ingredient-name
                        :cooked-with/ingredient
                        (some #(when (= (:ingredient/name %) ingredient-name)
                                 (:ingredient/id %))
                              all-ingredients)))))
       (remove (fn [{:keys [ingredient/name]}] (unused-ingredients name)))
       throw-for-unknown-ingredients))

(defmulti load-recipe (fn [link] (recipe-source link)))

(defmethod load-recipe :google-docs [link]
  (let [recipe-id ((str/split link #"/") 5)]
    (-> (oauth-token)
        (.then (fn [access-token]
                 (js/Promise.all #js [(js/fetch (str drive-api-url "/files/" recipe-id "/export?mimeType=text/plain")
                                                (clj->js {:headers {"authorization" (str "Bearer " access-token)}}))
                                      (js/fetch (str drive-api-url "/files/" recipe-id "?mimeType=text/plain")
                                                  (clj->js {:headers {"authorization" (str "Bearer " access-token)}}))])))
        (.then (fn [[recipe-res doc-res]] (js/Promise.all
                                          #js [(.text recipe-res)
                                               (.json doc-res)])))
        (.then (fn [[recipe doc]]
                 (-> (js->clj doc :keywordize-keys true)
                     (assoc :content recipe)))))))

(defmethod load-recipe :default [link]
  (as-hickory link))

(defn scrape-recipe2 [recipe]
  {:pre [(:recipe/link recipe)]}
  (-> (js/Promise.all #js [(load-recipe (:recipe/link recipe)) (load-ingredients)])
      (.then (fn [[recipe-hickory]]
               (let [scraped-recipe (cond-> recipe
                                      (not (:recipe/id recipe))          (assoc :recipe/id (str (random-uuid)))
                                      (not (:recipe/name recipe))        (assoc :recipe/name (recipe-name (:recipe/link recipe) recipe-hickory))
                                      (not (:recipe/type recipe))        (assoc :recipe/type :recipe-type/normal)
                                      (not (:recipe/image recipe))       (assoc :recipe/image (recipe-image (:recipe/link recipe) recipe-hickory))
                                      (not (:recipe/ingredients recipe)) (assoc :recipe/ingredients (->> (recipe-ingredients (:recipe/link recipe) recipe-hickory)
                                                                                                         (dedup-ingredients @ingredients)
                                                                                                         (mapv #(cooked-with/clean % @ingredients)))))]
                 (when-not (s/valid? :recipe/recipe scraped-recipe)
                   (throw (ex-info "Recipe is invalid" (s/explain-data :recipe/recipe scraped-recipe))))
                 scraped-recipe)))
      (.catch (fn [e] (js/console.error e (clj->js (ex-data e)))))))

(defn main [])

(defn save-recipe [recipe]
  {:pre [(s/valid? :recipe/recipe recipe)]}
  (firestore/setDoc (firestore/doc (firestore/collection db "recipes") (:recipe/id recipe))
                    (->js recipe)))
(comment
  (-> (scrape-recipe2 {:recipe/link "https://docs.google.com/document/d/1_nJLAumRYAs-yZVqkLEOLR7eXV5DwqMxnW965Azf5J8"})
      save-recipe))
