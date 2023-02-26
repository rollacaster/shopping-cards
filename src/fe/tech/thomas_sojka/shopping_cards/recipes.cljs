(ns tech.thomas-sojka.shopping-cards.recipes
  (:require [cljs.reader :as reader]
            [clojure.set :as set]
            [re-frame.core :refer [inject-cofx reg-cofx reg-event-db
                                   reg-event-fx reg-fx reg-sub]]
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
     :app/push-state [:route/recipes]}))

(def localstorage-recipes-key "recipes")

(reg-cofx :local-store-recipes
  (fn [cofx]
    (assoc cofx :local-store-recipes
           (some->> (.getItem js/localStorage localstorage-recipes-key)
                    (reader/read-string)))))

(reg-event-fx :recipes/load
  [(inject-cofx :local-store-recipes)]
  (fn [{:keys [local-store-recipes db]}]
    (if (empty? local-store-recipes)
      {:firestore/snapshot {:path firestore-path
                            :on-success [:recipes/load-success]
                            :on-failure [:recipes/load-failure]}}
      {:db (assoc db :recipes local-store-recipes)})))

(defn ->recipe [firestore-recipe]
  (cond-> firestore-recipe
      :always (set/rename-keys {:type :recipe/type})
      :always (update :recipe/type (fn [t] (keyword "recipe-type" t)))
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

(reg-fx :recipes/store
  (fn [recipes]
    (.setItem js/localStorage localstorage-recipes-key (str recipes))))

(reg-event-fx :recipes/load-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db :recipes (map ->recipe data))
     :recipes/store (map ->recipe data)}))

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
