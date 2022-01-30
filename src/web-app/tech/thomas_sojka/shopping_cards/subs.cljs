(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub]]
            ["date-fns" :refer (addDays startOfDay isAfter getDate getMonth)]
            [clojure.string :as str]))

(reg-sub
 :shopping-card/selected-ingredient-ids
 (fn [db _]
   (:shopping-card/selected-ingredient-ids db)))

(reg-sub
 :main/recipes
 (fn [db _]
   (:main/recipes db)))

(reg-sub
 :ingredients
 (fn [db _]
   (:ingredients db)))

(reg-sub
 :addable-ingredients
 :<- [:ingredients]
 :<- [:shopping-card/ingredients]
 :<- [:extra-ingredients/filter]
 (fn [[ingredients recipe-ingredients ingredient-filter] _]
   (->> ingredients
        (remove (fn [ingredient]
                  (or
                   ((set (map first recipe-ingredients))
                    (:id ingredient))
                   (not (str/includes? (str/lower-case (:name ingredient))
                                       (str/lower-case ingredient-filter)))))))))

(reg-sub
 :app/error
 (fn [db _]
   (:app/error db)))

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
 :<- [:main/recipes]
 sorted-recipes)

(defn lunch-recipes [recipes]
  (->> recipes
        (group-by :type)
        (sort-recipes ["FAST" "NEW" "NORMAL" "MISC" "RARE"])))

(reg-sub
 :lunch-recipes
 :<- [:main/recipes]
 lunch-recipes)

(reg-sub
 :recipe-details/ingredients
 (fn [db _]
   (:recipe-details/ingredients db)))

(reg-sub
 :shopping-card/ingredients
 (fn [db _]
   (:shopping-card/ingredients db)))

(reg-sub
 :app/route
 (fn [db _]
   (:app/route db)))

(reg-sub
 :app/loading
 (fn [db _]
   (:app/loading db)))

(reg-sub
 :main/meal-plans
 (fn [db _]
   (:main/meal-plans db)))

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
 :weekly-meal-plans
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
 :recipe-details/meal
 (fn [db _]
   (:recipe-details/meal db)))

(reg-sub
  :extra-ingredients/filter
  (fn [db _]
    (:extra-ingredients/filter db)))

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
