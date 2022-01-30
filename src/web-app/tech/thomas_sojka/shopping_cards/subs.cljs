(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub]]
            ["date-fns" :refer (addDays startOfDay isAfter getDate getMonth)]
            [clojure.string :as str]))

(reg-sub
 :selected-ingredients
 (fn [db _]
   (:selected-ingredients db)))

(reg-sub
 :recipes
 (fn [db _]
   (:recipes db)))

(reg-sub
 :ingredients
 (fn [db _]
   (:ingredients db)))

(reg-sub
 :addable-ingredients
 :<- [:ingredients]
 :<- [:recipe-ingredients]
 :<- [:ingredient-filter]
 (fn [[ingredients recipe-ingredients ingredient-filter] _]
   (->> ingredients
        (remove (fn [ingredient]
                  (or
                   ((set (map first recipe-ingredients))
                    (:id ingredient))
                   (not (str/includes? (str/lower-case (:name ingredient))
                                       (str/lower-case ingredient-filter)))))))))

(reg-sub
 :error
 (fn [db _]
   (:error db)))

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
        (sort-recipes ["NORMAL" "NEW" "MISC" "FAST" "RARE"])))

(reg-sub
 :sorted-recipes
 :<- [:recipes]
 sorted-recipes)

(defn lunch-recipes [recipes]
  (->> recipes
        (group-by :type)
        (sort-recipes ["FAST" "NEW" "NORMAL" "MISC" "RARE"])))

(reg-sub
 :lunch-recipes
 :<- [:recipes]
 lunch-recipes)

(reg-sub
 :recipe-details
 (fn [db _]
   (:recipe-details db)))

(reg-sub
 :recipe-ingredients
 (fn [db _]
   (:recipe-ingredients db)))

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
    (range 4))))

(reg-sub
 :selected-meal
 (fn [db _]
   (:selected-meal db)))

(reg-sub
  :ingredient-filter
  (fn [db _]
    (:ingredient-filter db)))

(reg-sub
 :bank-holidays
 (fn [db]
   (filter
    #(or (nil? (:states %)) ((:states %) :by))
    (:bank-holidays db))))

(reg-sub
 :bank-holiday
 :<- [:bank-holidays]
 (fn [bank-holidays [_ date]]
   (let [c-day (getDate date)
         c-month (getMonth date)]
     (some (fn [{:keys [month day name]}]
             (when
              (and (= month (inc c-month))
                   (= day c-day))
               name))
           bank-holidays))))
