(ns tech.thomas-sojka.shopping-cards.bank-holidays
  (:require ["date-fns" :refer (getDate getMonth)]
            [ajax.core :as ajax]
            [cljs.reader :refer [read-string]]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

(reg-event-fx :bank-holidays/load
 (fn [_ [_ year]]
   {:http-xhrio {:method :get
                 :uri (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                           year
                           ".edn")
                 :response-format (ajax/text-response-format)
                 :on-success [:bank-holidays/load-success]
                 :on-failure [:bank-holidays/load-failure]}}))

(reg-event-db :bank-holidays/load-success
 (fn [db [_ data]]
   (assoc db :bank-holidays (read-string data))))

(reg-event-fx :bank-holidays/load-failure
 (fn [{:keys [db]} _]
   {:db (assoc db :app/error "Fehler: Feiertage nicht geladen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-sub :bank-holidays/local
 (fn [db]
   (filter
    #(or (nil? (:states %)) ((:states %) :by))
    (:bank-holidays db))))

(reg-sub :bank-holiday
 :<- [:bank-holidays/local]
 (fn [bank-holidays [_ date]]
   (let [c-day (getDate date)
         c-month (getMonth date)]
     (some (fn [{:keys [month day name]}]
             (when
                 (and (= month (inc c-month))
                      (= day c-day))
               name))
           bank-holidays))))
