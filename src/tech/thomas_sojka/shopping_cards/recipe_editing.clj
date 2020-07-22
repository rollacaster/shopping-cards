(ns tech.thomas-sojka.shopping-cards.recipe-editing
  (:require [clj-http.client :as client]
            [clojure.set :refer [difference]]
            [clojure.string :as s]
            [clojure.core.async]
            [tech.thomas-sojka.shopping-cards.auth :refer [creds-file]]
            [tech.thomas-sojka.shopping-cards.db :as db]
            [tech.thomas-sojka.shopping-cards.scrape :as scrape]
            [tech.thomas-sojka.shopping-cards.trello :refer [trello-api]]))

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
         (take-while #(not= % "Selten"))
         (filter #(s/includes? % "- "))
         (map meal-line->clj))))

(defn added-recipes [trello-recipes]
  (let [added-names (difference
                     (set (map :name trello-recipes))
                     (set (map :name (db/load-recipes))))]
    (filter #(added-names (:name %)) (load-trello-recipes))))

(defn removed-recipes [trello-recipes]
  (difference
   (set (->> (db/load-recipes)
             (filter (comp not :inactive))
             (map :name)))
   (set (map :name trello-recipes))))

(defn mark-inactive-recipes [recipes remove-recipes]
  (db/write-edn "recipes.edn"
             (map #(assoc % :inactive (if (remove-recipes (:name %)) true false)) recipes)))

(defn show-recipe [recipe-id]
  (let [recipe (some (fn [{:keys [id] :as recipe}] (when (= id recipe-id) recipe))(db/load-recipes))
        cooked-with (filter #(= (:recipe-id %) (:id recipe)) (db/load-cooked-with))
        ingredients (map
                     #(some (fn [ingredient] (when (= (:ingredient-id %) (:id ingredient)) (merge ingredient %))) (db/load-ingredients))
                     cooked-with)]
    (assoc recipe :ingredients
           ingredients)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-ingredient-to-recipe [recipe-id {:keys [amount category name amount-desc unit]}]
  (let [ingredient-id (uuid)]
    {:recipe-id recipe-id :amount-desc amount-desc
     :amount amount :unit unit :ingredient-id ingredient-id
     :id (uuid)}
    {:id ingredient-id :name name :category category}))

(defn add-ingredient [{:keys [category name]}]
  (db/write-edn
   "ingredients.edn"
   (conj
    (db/load-ingredients)
    {:id (uuid)
     :name name
     :category category})))

(defn find-ingredient [ingredient-name]
  (some #(when (= (:name %) ingredient-name) (:id %)) (db/load-ingredients)))

(defn add-cooked-with [recipe-id ingredient-id {:keys [amount amount-desc unit] :or {amount nil amount-desc "" unit nil}}]
  (db/write-edn
   "cooked-with.edn"
   (conj
    (db/load-cooked-with)
    {:recipe-id recipe-id :amount-desc amount-desc
     :amount amount :unit unit :ingredient-id ingredient-id
     :id (uuid)})))

(defn remove-cooked-with [recipe-id ingredient-id]
  (db/write-edn
   "cooked-with.edn"
   (remove
    #(and (= recipe-id (:recipe-id %)) (= (:ingredient-id %) ingredient-id))
    (db/load-cooked-with))))

(defn add-new-recipe [{:keys [name link image type] :as new-recipe}]
  (let [recipe-id (uuid)
        cooked-with (db/load-cooked-with)
        recipes (db/load-recipes)]
    (db/write-edn
     "cooked-with.edn"
     (->> new-recipe
          :ingredients
          (map
           (fn [{:keys [amount-desc amount id]}]
             {:id (uuid) :amount-desc amount-desc :amount amount :ingredient-id id :recipe-id recipe-id}))
          (concat cooked-with)
          vec))
    (db/write-edn "recipes.edn"
               (conj recipes {:id recipe-id :name name :link link :image image :type type}))))

(defn remove-ingredient [recipe ingredient-name]
  (update recipe :ingredients #(remove (fn [ingredient] (= ingredient-name (:name ingredient))) %)))

(defn find-recipe [recipe-name]
  (some #(when (= (:name %) recipe-name) (:id %)) (db/load-recipes)))
(let [types (->> (load-trello-recipes)
                 (map #(assoc % :type (if (#{"Sojka Sandwiches"
                                             "Käse-Brezeln"
                                             "Camenbert"
                                             "Ofenkäse"
                                             "Spiegelei mit Spinat"
                                             "Brezel + Tofu"
                                             "Gebackender Feta"
                                             "Spätzle mit Ei"
                                             "Tortellini mit Pesto"} (:name %)) "FAST" "NORMAL")))
                 (map #(assoc % :id (find-recipe (:name %))))
                 (filter :id)
                 (map #(select-keys % [:id :type])))]
  (db/write-edn
   "recipes.edn"
   (map
    (fn [[_ lists]]
      (apply merge lists))
    (merge-with
     concat
     (group-by :id (db/load-recipes))
     (group-by :id types)))))



(comment
  (add-cooked-with (find-recipe "Avocado-Pesto") (find-ingredient "Parmesan") {:amount nil :amount-desc "" :unit nil})
  (add-new-recipe
   {:name "Couscous"
    :link ""
    :image "https://www.jessicagavin.com/wp-content/uploads/2019/03/mediterranean-couscous-salad-2-600x900.jpg"
    :inactive false
    :type "Normal"
    :ingredients [{:id (find-ingredient "Paprika") :amount 2 :amount-desc "2" :unit nil}
                  {:id (find-ingredient "Karotte") :amount 6 :amount-desc "6" :unit nil}
                  {:id (find-ingredient "Mais") :amount 1 :amount-desc "1 Dose" :unit nil}
                  {:id (find-ingredient "Feta") :amount 1 :amount-desc "1" :unit nil}
                  {:id (find-ingredient "Couscous") :amount 250 :amount-desc "250 g" :unit "g"}
                  {:id (find-ingredient "Zitronensaft") :amount nil :amount-desc "etwas" :unit nil}]})
  (find-ingredient "Eier")
  (let [new-recipe (->> (load-trello-recipes)
                        added-recipes
                        #_(drop 1)
                        #_(take 1)
                        #_first
                        (map :name))]
    (-> new-recipe
        (assoc :image "https://www.ditsch.de/mcinfo_assets/de/51d7fa67d7bf0a7db660b93b3530605eb3b222ff.jpeg")
        (assoc :ingredients [{:name "Spätzle" :amount 500 :amount-desc "500 g" :unit "g"}
                             {:name "Eier" :amount 2 :amount-desc nil :unit nil}])
        scrape/dedup-ingredients
        #_scrape/find-image))
  (add-ingredient {:category "Backen" :name "Mandelsplitter"})
  (add-new-recipe
   (scrape/add-chefkoch-recipe {:link "https://www.chefkoch.de/rezepte/3232941480954040/Gefuellte-Paprika-mit-Feta-und-Mozzarella.html"
                                :type "FAST"})))
