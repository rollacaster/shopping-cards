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
  (fn []
    {:firestore/snapshot {:path firestore-path
                          :on-success [:recipes/load-success]
                          :on-failure [:recipes/load-failure]}}))

(defn ->recipe [firestore-recipe]
  (cond-> firestore-recipe
      :always (set/rename-keys {:id :recipe/id
                                :name :recipe/name
                                :type :recipe/type
                                :image :recipe/image
                                :link :recipe/link
                                :ingredients :recipe/ingredients})
      :always (update :recipe/type (fn [t] (keyword "recipe-type" t)))
      (:ingredients firestore-recipe)
      (update :recipe/ingredients (fn [cooked-with]
                                    (mapv
                                     (fn [c]
                                       (-> c
                                           (set/rename-keys {:unit :cooked-with/unit
                                                             :amount-desc :cooked-with/amount-desc
                                                             :amount :cooked-with/amount
                                                             :ingredient :ingredient/ingredient})
                                           (update :ingredient/ingredient ingredients/->ingredient)))
                                     cooked-with)))))
(reg-event-fx :recipes/load-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db :recipes (map ->recipe data))}))

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
     (fn [{:keys [id] :as recipe}] (when (= id recipe-id) recipe))
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
