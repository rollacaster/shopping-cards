(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :selected-recipes
 (fn [db _]
   (:selected-recipes db)))

(reg-sub
 :selected-ingredients
 (fn [db _]
   (:selected-ingredients db)))

(reg-sub
 :recipes
 (fn [db _]
   (:recipes db)))

(def type-order ["NORMAL" "FAST" "RARE"])

(defn sort-recipes [type-order recipes]
  (sort-by
   (fn [[recipe-type]] (->> type-order
                           (map-indexed #(vector %1 %2))
                           (some
                            (fn [[idx recipe-type-order]]
                              (when (= recipe-type-order recipe-type) idx)))))
   recipes))

(defn sorted-recipes [recipes]
  (->> recipes
        (group-by :type)
        (sort-recipes ["NORMAL" "FAST" "RARE"])))

(reg-sub
 :sorted-recipes
 :<- [:recipes]
 sorted-recipes)

(defn lunch-recipes [recipes]
  (->> recipes
        (group-by :type)
        (sort-recipes ["FAST" "NORMAL" "RARE"])))

(reg-sub
 :lunch-recipes
 :<- [:recipes]
 lunch-recipes)

(reg-sub
 :recipe-details
 (fn [db _]
   (:recipe-details db)))

(reg-sub
 :shown-recipe
 :<- [:recipes]
 (fn [recipes [_ recipe-id]]
   (->> recipes
        (some #(when (= (:id %) recipe-id) %)))))

(reg-sub
 :ingredients
 (fn [db _]
   (:ingredients db)))

(reg-sub
 :route
 (fn [db _]
   (:route db)))

(reg-sub
 :loading
 (fn [db _]
   (:loading db)))

(reg-sub
 :meal-plans
 (fn [db _]
   (:meal-plans db)))

(reg-sub
 :meal-plans-with-recipes
 :<- [:meal-plans]
 :<- [:recipes]
 (fn [[meal-plans recipes]]
   (map
    (fn [meal-plan]
      (assoc
       meal-plan
       :recipe
       (some #(when (= (:id %) (:recipe-id meal-plan)) %) recipes)))
    meal-plans)))

(comment
  @(subscribe [:meal-plans])
  @(subscribe [:recipes])
  @(subscribe [:meal-plans-with-recipes])

  @(subscribe [:meal-plans])
  @(subscribe [:selected-recipes])
  @(subscribe [:selected-ingredients])
  @(subscribe [:shown-recipe "9bbdb4ef-4934-4a96-be22-881ed37c0fd5"])

  @(subscribe [:recipes])
  @(subscribe [:sorted-recipes])
  @(subscribe [:recipe-details])
  @(subscribe [:ingredients])
  @(subscribe [:route])
  @(subscribe [:loading]))
