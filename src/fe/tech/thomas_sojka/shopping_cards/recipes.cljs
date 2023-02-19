(ns tech.thomas-sojka.shopping-cards.recipes
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
            [tech.thomas-sojka.shopping-cards.ingredients :as ingredients]))

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

(defn sort-recipes [type-order recipes]
  (sort-by
   (fn [[recipe-type]] (->> type-order
                           (map-indexed #(vector %1 %2))
                           (some
                            (fn [[idx recipe-type-order]]
                              (when (= recipe-type-order recipe-type) idx)))))
   recipes))

(defn lunch-recipes [recipes]
  (->> recipes
       (group-by :recipe/type)
       (sort-recipes [:recipe-type/fast :recipe-type/new :recipe-type/normal :recipe-type/misc  :recipe-type/rare])))

(reg-sub :recipes/lunch
 :<- [:recipes]
 lunch-recipes)

(defn dinner-recipes [recipes]
  (->> recipes
       (group-by :recipe/type)
       (sort-recipes [:recipe-type/normal :recipe-type/fast :recipe-type/new :recipe-type/misc  :recipe-type/rare])))

(reg-sub :recipes/dinner
 :<- [:recipes]
 dinner-recipes)

(reg-event-fx :recipes/add
  (fn [_ [_ recipe]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "recipes"
                           :data (assoc recipe :id new-id)
                           :on-success [:recipes/add]
                           :on-failure [:recipes/add-failure]}})))

(reg-event-fx :recipes/add-success
  (fn []
    {:app/push-state [:route/main]}))

(reg-event-fx :recipes/add-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx :recipes/remove
  (fn [_ [_ recipe-id]]
    (def recipe-id ){:firestore/remove-doc {:path "(first reciprecipees"
                              :key recipe-id
                              :on-success [:recipes/remove-success]
                              :on-failure [:recipes/remove-failure]}}))

(reg-event-fx :recipes/remove-success
  (fn []
    {:app/push-state [:route/recipe]}))

(reg-event-fx :recipes/remove-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Löschen fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))


(reg-event-fx :recipes/load
  (fn []
    {:firestore/snapshot {:path "recipes"
                          :on-success [:recipes/load-success]
                          :on-failure [:recipes/load-failure]}}))

(defn ->recipe [firestore-recipe]
  (cond-> firestore-recipe
      :always (set/rename-keys {:type :recipe/type})
      :always(update :recipe/type (fn [t] (keyword "recipe-type" t)))
      (:ingredients firestore-recipe)
      (update :ingredients (fn [cooked-with]
                             (map
                              (fn [{:keys [ingredient] :as c}]
                                [(-> c
                                     (set/rename-keys {:unit :cooked-with/unit
                                                       :amount-desc :cooked-with/amount-desc
                                                       :amount :cooked-with/amount})
                                     (dissoc :ingredient))
                                 (ingredients/->ingredient ingredient)])
                              cooked-with)))))

(reg-event-db :recipes/load-success
 (fn [db [_ data]]
   (assoc db :recipes (map ->recipe data))))

(reg-event-db :main/failure-recipes
 (fn [db _]
   (assoc db :recipes :ERROR)))
