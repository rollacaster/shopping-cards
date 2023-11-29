(ns tech.thomas-sojka.shopping-cards.recipes
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]
            [clojure.spec.alpha :as s]))

(def firestore-path "recipes")

(defn ->firestore-recipe [recipe]
  {:post [(s/valid? :recipe/recipe %)]}
  (-> recipe
      (update :recipe/cooked-with
              (fn [cooked-with]
                (mapv
                 (fn [c]
                   (apply dissoc c
                          (filter
                           (fn [key]
                             (and (= (namespace key) "ingredient") (not= key :ingredient/id)))
                           (keys c))))
                 cooked-with)))))

(reg-event-fx :recipes/update
  (fn [_ [_ {:recipe/keys [id] :as recipe}]]
    {:firestore/update-doc {:path firestore-path
                            :key id
                            :data (->firestore-recipe recipe)}
     :app/push-state [:route/edit-recipes]
     :app/scroll-to [0 0]}))

(reg-event-fx :recipes/load
  (fn [_ [_ ingredients]]
    {:firestore/snapshot {:path firestore-path
                          :on-success [:recipes/load-success ingredients]
                          :on-failure [:recipes/load-failure]}}))

(defn- explode-ingredients [cooked-with ingredients]
  (let [ingredient-id->ingredient (zipmap (map :ingredient/id ingredients)
                                          ingredients)]
    (mapv (fn [c] (merge c (ingredient-id->ingredient (:cooked-with/ingredient c))))
          cooked-with)))

(defn ->recipe [ingredients firestore-recipe]
  (-> firestore-recipe
      (update :recipe/type keyword)
      (update :recipe/cooked-with explode-ingredients ingredients)))

(reg-event-fx :recipes/load-success
  (fn [{:keys [db]} [_ ingredients data]]
    {:db (assoc db :recipes (map (partial ->recipe ingredients) data))}))

(reg-event-db :recipes/load-failure
 (fn [db _]
   (assoc db :recipes :ERROR)))

(reg-sub :recipes
 (fn [db _]
   (:recipes db)))

(defn find-recipe [recipe-id recipes]
  (some
   (fn [{:recipe/keys [id] :as recipe}] (when (= id recipe-id) recipe))
   recipes))

(reg-sub :recipes/details
  :<- [:recipes]
  (fn [recipes [_ recipe-id]]
    (find-recipe recipe-id recipes)))

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
