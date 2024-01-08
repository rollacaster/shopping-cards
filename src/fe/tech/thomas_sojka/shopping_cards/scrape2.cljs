(ns tech.thomas-sojka.shopping-cards.scrape2
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
            [tech.thomas-sojka.shopping-cards.specs]))

(def firebaseConfig
  #js {:apiKey "AIzaSyAXNnWku5rgm33z6FtDoJu-IEEf1Y7RC1I"
       :authDomain "shopping-cards-e24af.firebaseapp.com"
       :projectId "shopping-cards-e24af"
       :storageBucket "shopping-cards-e24af.appspot.com"
       :messagingSenderId "235874496901"
       :appId "1:235874496901:web:dd7f231adee24e4b8f54ec"})

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
       {:headers {"Accept-Language" "de-DE,de;q=0.9,en-DE;q=0.8,en;q=0.7,en-US;q=0.6"}})
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

(defn ingredient-name [name]
  (some (fn [[ingredient-group-name duplicated-name]]
          (when (or (= name ingredient-group-name)
                    (contains? duplicated-name name))
            ingredient-group-name))
        (edn/read-string (.toString (fs/readFileSync "resources/duplicated-ingredients.edn")))))

(defn throw-for-unknown-ingredients [ingredients]
  (let [unknown-ingredients (remove :cooked-with/ingredient ingredients)]
    (if (seq unknown-ingredients)
      (throw (ex-info "Unkown ingredients found!"
                      {:unknown-ingredients (map :ingredient/name unknown-ingredients)}))
      ingredients)))

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
       throw-for-unknown-ingredients))

(defn scrape-recipe2 [recipe]
  {:pre [(:recipe/link recipe)]}
  (-> (js/Promise.all #js [(as-hickory (:recipe/link recipe)) (load-ingredients)])
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
  (firestore/setDoc (firestore/doc (firestore/collection db "recipes") (:recipe/id recipe))
                    (->js recipe)))
(comment
  (-> (scrape-recipe2
       {:recipe/link "https://www.gaumenfreundin.de/smashed-brokkoli/"
        :recipe/type :recipe-type/normal})
      (.then (fn [recipe] (save-recipe recipe)))
      (.catch (fn [err] (js/console.log err))))
  )
