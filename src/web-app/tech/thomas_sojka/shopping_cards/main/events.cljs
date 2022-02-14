(ns tech.thomas-sojka.shopping-cards.main.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [ajax.core :as ajax]
            [cljs.reader :refer [read-string]]
            ["date-fns" :refer (startOfDay format)]))

(reg-event-fx
 :main/load-recipes
 (fn [{:keys [db]}]
   (if (empty? (:main/recipes db))
     {:db (assoc db :app/loading true)
      :http-xhrio {:method :get
                   :uri "recipes"
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:main/success-recipes]
                   :on-failure [:main/failure-recipes]}}
     {:db db})))

(reg-event-db
 :main/success-recipes
 (fn [db [_ data]]
   (-> db
       (assoc :app/loading false)
       (assoc :main/recipes data))))

(reg-event-db
 :main/failure-recipes
 (fn [db _]
   (-> db
       (assoc :app/loading false)
       (assoc :main/recipes :ERROR))))


(defn meal-plans-loaded-for-today? [db today]
  (->> (:main/meal-plans db)
       (map #(:date %))
       (some #(= today %))))

(reg-event-fx
 :main/init-meal-plans
 (fn [{:keys [db]} [_ today]]
   (if (meal-plans-loaded-for-today? db today)
     {:db (assoc db :main/start-of-week (startOfDay today))}
     {:db (-> db
              (assoc :app/loading true)
              (assoc :main/start-of-week today))
      :http-xhrio {:method :get
                   :uri (str "/meal-plans/" (format today "yyyy-MM-dd"))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:main/success-meal-plans]
                   :on-failure [:main/failure-meal-plans]}})))

(reg-event-db
 :main/success-meal-plans
 (fn [db [_ data]]
   (-> db
       (assoc :app/loading false)
       (update :main/meal-plans
               concat
               (map (fn [meal-plan]
                      (-> meal-plan
                          (update :type keyword)
                          (update :date #(js/Date. %))))
                    data)))))

(reg-event-db
 :main/failure-meal-plans
 (fn [db _]
   (-> db
       (assoc :app/loading false)
       (assoc :main/meal-plans :ERROR))))

(reg-event-fx
 :main/add-meal
 (fn [{:keys [db]} [_ recipe]]
   (prn :main/add-meal recipe)
   {:db (-> db
            (update :main/meal-plans conj (assoc (:recipe-details/meal db) :recipe recipe))
            (assoc :recipe-details/meal nil))
    :app/push-state [:route/main]
    :http-xhrio {:method :post
                 :uri "/meal-plans"
                 :params (assoc (:recipe-details/meal db) :recipe recipe)
                 :format (ajax/json-request-format)
                 :response-format (ajax/text-response-format)
                 :on-failure [:main/failure-add-meal (:recipe-details/meal db)]}}))

(reg-event-db
 :main/success-bank-holidays
 (fn [db [_ data]]
   (assoc db :main/bank-holidays (read-string data))))

(reg-event-fx
 :main/failure-bank-holidays
 (fn [{:keys [db]} _]
   {:db (assoc db :app/error "Fehler: Feiertage nicht geladen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(defn remove-meal [meal-plans {:keys [date type]}]
  (remove (fn [m] (and (= date (:date m))
                      (= type (:type m))))
          meal-plans))

(reg-event-fx
 :main/failure-add-meal
 (fn [{:keys [db]} [_ failed-meal]]
   {:db (-> db
            (update :main/meal-plans remove-meal failed-meal)
            (assoc :app/error "Fehler: Speichern fehlgeschlagen"))
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :main/select-meal
 (fn [{:keys [db]} [_ meal]]
   {:app/push-state
    (if (= (:type meal) :meal-type/lunch)
      [:route/select-lunch]
      [:route/select-dinner])
    :db (assoc db :recipe-details/meal meal)}))

(reg-event-fx
 :main/restart
 (fn [{:keys [db]} _]
   {:app/push-state [:route/main]
    :db (assoc db :shopping-card/selected-ingredient-ids #{})}))

(reg-event-fx
 :main/remove-meal
 (fn [{:keys [db]}]
   (let [{:keys [date type]} (:recipe-details/meal db)]
     {:db
      (update db :main/meal-plans remove-meal (:recipe-details/meal db))
      :app/push-state [:route/meal-plan]
      :http-xhrio {:method :delete
                   :uri "/meal-plans"
                   :url-params {:date (.toISOString date) :type type}
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-failure [:main/failure-remove-meal (:recipe-details/meal db)]}})))

(reg-event-fx
 :main/failure-remove-meal
 (fn [{:keys [db]} [_ failed-meal]]
   {:db
    (-> db
        (update :main/meal-plans conj failed-meal)
        (assoc :app/error "Fehler: Löschen fehlgeschlagen"))
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))
