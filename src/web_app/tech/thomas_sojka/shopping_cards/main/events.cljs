(ns tech.thomas-sojka.shopping-cards.main.events
  (:require ["date-fns" :refer [format startOfDay]]
            [cljs.reader :refer [read-string]]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-db
 :main/success-recipes
 (fn [db [_ data]]
   (-> db
       (assoc :app/loading false)
       (assoc :main/recipes (->> data
                                 (map (fn [r] (-> r
                                                 first
                                                 (update :recipe/type :db/ident)))))))))

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
      :db/datascript [:query {:q '[:find (pull ?m [[:meal-plan/inst :as :date]
                                              [:meal-plan/id :as :id]
                                              {[:meal-plan/type :as :type]
                                               [[:db/ident :as :ref]]}
                                              {[:meal-plan/recipe :as :recipe]
                                               [[:recipe/id :as :id]
                                                [:recipe/name :as :name]
                                                {:recipe/type [[:db/ident]]}
                                                [:recipe/image :as :image]
                                                [:recipe/link :as :link]]}
                                              [:shopping-list/_meals :as :shopping-list]])
                              :in $ ?date
                              :where [?m :meal-plan/inst ?d]]
                         :params (format today "yyyy-MM-dd")
                         :on-success [:main/success-meal-plans]
                         :on-failure [:main/failure-meal-plans]}]})))

(reg-event-db
 :main/success-meal-plans
 (fn [db [_ data]]
   (-> db
       (assoc :app/loading false)
       (update :main/meal-plans
               concat
               (map (fn [meal-plan]
                      (-> meal-plan
                          first
                          (update :type :ref)
                          (update :date #(js/Date. %))
                          (update-in [:recipe :recipe/type] :db/ident)))
                    data)))))

(reg-event-fx
 :main/failure-meal-plans
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/loading false
           :app/error "Fehler: Essen nicht geladen."
           :main/meal-plans [])
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :main/add-meal
 (fn [{:keys [db]} [_ recipe]]
   ;; TODO Move to interceptor
   (let [new-id (str (random-uuid))]
     {:db (-> db
              (update :main/meal-plans conj (assoc (:recipe-details/meal db)
                                                   :recipe recipe :id new-id))
              (assoc :recipe-details/meal nil))
      :app/push-state [:route/main]
      :dispatch [:transact {:tx-data [(let [{:keys [date type]} (:recipe-details/meal db)]
                                        {:meal-plan/id new-id
                                         :meal-plan/inst date
                                         :meal-plan/type type
                                         :meal-plan/recipe [:recipe/id (:id recipe)]})]
                            :on-success [:main/success-add-meal]
                            :on-failure [:main/failure-add-meal (:recipe-details/meal db)]}]})))

(reg-event-db
  :main/success-add-meal
  identity)

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

(defn remove-meal [meal-plans {:keys [id]}]
  (remove (fn [m] (= id (:id m))) meal-plans))

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
   (let [{:keys [id]} (:recipe-details/meal db)]
     {:db (update db :main/meal-plans remove-meal (:recipe-details/meal db))
      :app/push-state [:route/meal-plan]
      :dispatch [:transact {:tx-data [[:db/retractEntity [:meal-plan/id id]]]
                            :on-success [:main/success-remove-meal]
                            :on-failure [:main/failure-remove-meal (:recipe-details/meal db)]}]})))

(reg-event-db
  :main/success-remove-meal
  identity)

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
