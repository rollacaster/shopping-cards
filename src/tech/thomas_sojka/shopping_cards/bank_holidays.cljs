(ns tech.thomas-sojka.shopping-cards.bank-holidays
  (:require ["date-fns" :refer (getDate getMonth)]
            [re-frame.core :refer [reg-event-db reg-sub]]))

(reg-event-db :bank-holidays/init
 (fn [db]
   (assoc db :bank-holidays #{{:name "Neujahr"
                               :month 1
                               :day 1}
                              {:name "Heilige drei Könige"
                               :month 1
                               :day 6}
                              {:name "Karfreitag"
                               :month 4
                               :day 18}
                              {:name "Ostermontag"
                               :month 4
                               :day 21}
                              {:name "Tag der Arbeit"
                               :month 5
                               :day 1}
                              {:name "Christi Himmelfahrt"
                               :month 5
                               :day 29}
                              {:name "Pfingstmontag"
                               :month 6
                               :day 9}
                              {:name "Fronleichnam"
                               :month 6
                               :day 19}
                              {:name "Mariä Himmelfahrt"
                               :month 8
                               :day 15}
                              {:name "Tag der deutschen Einheit"
                               :month 10
                               :day 3}
                              {:name "Allerheiligen"
                               :month 11
                               :day 1}
                              {:name "1. Weihnachtsfeiertag"
                               :month 12
                               :day 25}
                              {:name "2. Weihnachtsfeiertag"
                               :month 12
                               :day 26}})))

(reg-sub :bank-holidays/bavaria
 (fn [db]
   (filter
    #(or (nil? (:states %)) ((:states %) :by))
    (:bank-holidays db))))

(reg-sub :bank-holiday
 :<- [:bank-holidays/bavaria]
 (fn [bank-holidays [_ date]]
   (let [c-day (getDate date)
         c-month (getMonth date)]
     (some (fn [{:keys [month day name]}]
             (when
                 (and (= month (inc c-month))
                      (= day c-day))
               name))
           bank-holidays))))
