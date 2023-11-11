(ns tech.thomas-sojka.shopping-cards.recipes
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
            [tech.thomas-sojka.shopping-cards.ingredients :as ingredients]))

(def firestore-path "recipes")

(defn ->firestore-recipe [recipe]
  (update recipe :ingredients
          (fn [ingredients]
            (map
             (fn [[cooked-with ingredient]]
               (assoc cooked-with :ingredient ingredient))
             ingredients))))

(reg-event-fx :recipes/update
  (fn [_ [_ {:keys [id] :as recipe}]]
    {:firestore/update-doc {:path firestore-path
                            :key id
                            :data (->firestore-recipe recipe)}
     :app/push-state [:route/edit-recipes]}))

(reg-event-fx :recipes/load
  (fn [_ [_ ingredients]]
    (let [ingredient-id->ingredient (zipmap (map :ingredient/id ingredients) ingredients)]
      {:firestore/snapshot {:path firestore-path
                            :on-success [:recipes/load-success ingredient-id->ingredient]
                            :on-failure [:recipes/load-failure]}})))

(defn ->recipe [ingredient-id->ingredient firestore-recipe]
  (-> firestore-recipe
      (update :recipe/type keyword)
      (update :recipe/cooked-with (fn [cooked-with]
                                    (mapv
                                     (fn [c]
                                       (-> c
                                           (dissoc :cooked-with/ingredient)
                                           (merge (ingredient-id->ingredient
                                                   (:cooked-with/ingredient c)))))
                                     cooked-with)))))

(reg-event-fx :recipes/load-success
  (fn [{:keys [db]} [_ ingredients data]]
    {:db (assoc db :recipes (map (partial ->recipe ingredients) data))}))

(reg-event-db :recipes/load-failure
 (fn [db _]
   (assoc db :recipes :ERROR)))

(reg-sub :recipes
 (fn [db _]
   (:recipes db)))

(reg-sub :recipes/details
  :<- [:recipes]
  (fn [recipes [_ recipe-id]]
    (some
     (fn [{:recipe/keys [id] :as recipe}] (when (= id recipe-id) recipe))
     recipes)))

(reg-sub :recipes/recipe-types
 :<- [:recipes]
 (fn [recipes]
   (set (map :recipe/type recipes))))

(defn group-recipes [order recipes]
  (->> recipes
       (group-by :recipe/type)
       (sort-by
        (fn [[recipe-type]] (->> order
                                (map-indexed #(vector %1 %2))
                                (some
                                 (fn [[idx recipe-type-order]]
                                   (when (= recipe-type-order recipe-type) idx))))))))

(reg-sub :recipes/lunch
 :<- [:recipes]
  (fn [recipes]
    (group-recipes [:recipe-type/fast :recipe-type/new :recipe-type/normal :recipe-type/misc  :recipe-type/rare]
                   recipes)))

(reg-sub :recipes/dinner
 :<- [:recipes]
  (fn [recipes]
    (group-recipes [:recipe-type/normal :recipe-type/fast :recipe-type/new :recipe-type/misc  :recipe-type/rare]
                   recipes)))
