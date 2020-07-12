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

(defn add-ingredient [recipe-id {:keys [amount category name amount-desc unit]}]
  (let [ingredient-id (uuid)]
    {:recipe-id recipe-id :amount-desc amount-desc
     :amount amount :unit unit :ingredient-id ingredient-id
     :id (uuid)}
    {:id ingredient-id :name name :category category}))

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

(defn add-new-recipe [{:keys [name link image] :as new-recipe}]
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
               (conj recipes {:id recipe-id :name name :link link :image image}))))

(defn dedup-ingredients [recipe]
  (update recipe :ingredients (fn [ingredients]
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

(defn remove-ingredient [recipe ingredient-name]
  (update recipe :ingredients #(remove (fn [ingredient] (= ingredient-name (:name ingredient))) %)))



(comment
  (->
   (first (filter :link (added-recipes (load-trello-recipes))))
   scrape/add-ingredients
   (remove-ingredient "Hähnchenbrustfilet, roh")
   dedup-ingredients
   #_scrape/find-image
   (assoc :link "https://www.weightwatchers.de/images/1031/dynamic/foodandrecipes/2014/05/SpaetzeleHaehnchenauflauf2_800x800.jpg")
   add-new-recipe))
