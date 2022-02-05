(ns tech.thomas-sojka.shopping-cards.main.subs
  (:require [re-frame.core :refer [reg-sub]]
            ["date-fns" :refer (addDays startOfDay isAfter getDate getMonth addDays)]))

(reg-sub
 :main/recipes
 (fn [db _]
   (:main/recipes db)))

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
 :main/sorted-recipes
 :<- [:main/recipes]
 sorted-recipes)

(defn lunch-recipes [recipes]
  (->> recipes
       (group-by :type)
       (sort-recipes ["FAST" "NEW" "NORMAL" "MISC" "RARE"])))

(reg-sub
 :main/lunch-recipes
 :<- [:main/recipes]
 lunch-recipes)

(reg-sub
 :main/meal-plans
 (fn [db _]
   (:main/meal-plans db)))

(reg-sub
 :main/meals-without-shopping-list
 :<- [:main/weekly-meal-plans]
 (fn [meals-plans]
   (filter #(and (not (:shopping-list %))
                 (:recipe %)
                 (or (isAfter (:date %) (startOfDay (js/Date.)))
                     (= (:date %) (startOfDay (js/Date.)))))
           (flatten meals-plans))))

(reg-sub
 :main/start-of-week
 (fn [db _]
   (:main/start-of-week db)))

(defn group-meal-plans [meal-plans]
  (->> meal-plans
       (group-by :date)
       (map #(hash-map (first %) (->> (second %)
                                      (group-by :type)
                                      (map (fn [[type [recipe]]] (hash-map type recipe)))
                                      (apply merge))))
       (apply merge)))

(reg-sub
 :main/weekly-meal-plans
 :<- [:main/meal-plans]
 :<- [:main/start-of-week]
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
 :main/bank-holidays
 (fn [db]
   (filter
    #(or (nil? (:states %)) ((:states %) :by))
    (:main/bank-holidays db))))

(reg-sub
 :main/bank-holiday
 :<- [:main/bank-holidays]
 (fn [bank-holidays [_ date]]
   (let [c-day (getDate date)
         c-month (getMonth date)]
     (some (fn [{:keys [month day name]}]
             (when
                 (and (= month (inc c-month))
                      (= day c-day))
               name))
           bank-holidays))))
