(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]
            ["date-fns" :refer (addDays startOfDay isAfter)]))

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

(reg-sub
 :error
 (fn [db _]
   (:error db)))

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
 :meals-without-shopping-list
 :<- [:weekly-meal-plans]
 (fn [meals-plans]
   (filter #(and (not (:shopping-list %))
                 (:recipe %)
                 (or (isAfter (:date %) (startOfDay (js/Date.)))
                     (= (:date %) (startOfDay (js/Date.)))))
           (flatten meals-plans))))

(reg-sub
 :start-of-week
 (fn [db _]
   (:start-of-week db)))

(defn meal-plan->event [meal-plan]
  {:title (-> meal-plan :recipe :name)
   :start (:date meal-plan)
   :end (:date meal-plan)
   :resource {:recipe (:recipe meal-plan)
              :type (:type meal-plan)}})

(defn group-meal-plans [meal-plans]
  (->> meal-plans
       (group-by :date)
       (map #(hash-map (first %) (->> (second %)
                                      (group-by :type)
                                      (map (fn [[type [recipe]]] (hash-map type recipe)))
                                      (apply merge))))
       (apply merge)))

(reg-sub
 :weekly-meal-plans
 :<- [:meal-plans]
 :<- [:start-of-week]
 (fn [[meal-plans start-date]]
   (map
    (fn [day]
      (let [date (startOfDay (addDays start-date day))]
        [(get-in (group-meal-plans meal-plans)
                 [date :meal-type/lunch]
                 {:date date
                  :type :meal-type/lunch})
         (get-in (group-meal-plans meal-plans)
                 [date :meal-type/dinner]
                 {:date date
                  :type :meal-type/dinner})]))
    (range 6))))


(comment
  @(subscribe [:meal-plan-events])
  (get-in
   (->> @(subscribe [:meal-plans])
        (group-by :date)
        (map #(hash-map (first %) (group-by :type (second %))))
        first)
   [#inst "2021-11-02T23:00:00.000-00:00" :meal-type/dinner])
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
