(ns tech.thomas-sojka.shopping-cards.meal-plans
  (:require ["date-fns" :refer (addDays startOfDay isAfter addDays isEqual format)]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub reg-sub]]
            [tech.thomas-sojka.shopping-cards.recipes :as recipe]))

(reg-event-fx :meal-plans/show-details
  (fn [_ [_ meal-id]]
    {:app/push-state [:route/meal-plan-details {:meal-id meal-id}]}))

(reg-sub :meals-plans/meals
 (fn [db _]
   (:meals-plans/meals db)))

(defn group-meal-plans [meal-plans]
  (->> meal-plans
       (group-by :date)
       (map #(hash-map (first %) (->> (second %)
                                      (group-by :type)
                                      (map (fn [[type [recipe]]] (hash-map type recipe)))
                                      (apply merge))))
       (apply merge)))

(reg-sub :meal-plans/weekly
  :<- [:meals-plans/meals]
  :<- [:app/start-of-week]
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

(reg-sub :meals-plans/meals-without-shopping-list
 :<- [:meals-plans/meals]
 (fn [meals-plans]
   (filter
    (fn [{:keys [date shopping-list]}]
      (and
       (not shopping-list)
       (or (isAfter (startOfDay date) (startOfDay (js/Date.)))
           (isEqual (startOfDay date) (startOfDay (js/Date.))))))
    meals-plans)))

(reg-event-fx :meal/add
  (fn [ [_ meal]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "meal-plans"
                           :data (-> meal
                                     (assoc :id new-id)
                                     (update :recipe dissoc :ingredients))
                           :on-success [:meal/add-success]
                           :on-failure [:meal/add-failure]}})))

(reg-event-fx :meal/add-success
  (fn [{:keys [db]}]
    {:db (assoc db :recipe-details/meal nil)
     :app/push-state [:route/main]}))

(reg-event-fx :meal/add-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx :meal/remove
  (fn [_ [_ meal-plan-id]]
    {:firestore/remove-doc {:path "meal-plans"
                            :key meal-plan-id
                            :on-success [:meal/remove-success]
                            :on-failure [:meal/remove-failure]}}))

(reg-event-fx :meal/remove-success
  (fn []
    {:app/push-state [:route/meal-plan]}))

(reg-event-fx :meal/remove-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Löschen fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(defn- meal-plans-loaded-for-today? [db today]
  (->> (:meals-plans/meals db)
       (map #(:date %))
       (some #(= today %))))

(defn- attach-ingredients [db meal]
  (assoc meal :recipe (first
                       (get (->> (:main/recipes db) (group-by :id))
                            (:id (:recipe meal))))))

(reg-sub :meal/details
  (fn [db [_ meal-id]]
    (some
     (fn [{:keys [id] :as meal-plan}] (when (= id meal-id)
                                       (attach-ingredients db meal-plan)))
     (:meals-plans/meals db))))

(reg-event-fx :meal-plans/select-meal
 (fn [_ [_ meal]]
   (let [query-param-meal (-> meal
                              (update :date (fn [d] (format d "yyyy-MM-dd")))
                              (update :type name))]
     {:app/push-state
      (if (= (:type meal) :meal-type/lunch)
        [:route/select-lunch nil query-param-meal]
        [:route/select-dinner nil query-param-meal])})))

(reg-event-fx :meals/load (fn [{:keys [db]} [_ today]]
   (if (meal-plans-loaded-for-today? db today)
     {:db db}
     {:firestore/snapshot {:path "meal-plans"
                           :on-success [:meal/load-success]
                           :on-failure [:meal/load-failure]}})))

(reg-event-db :meal/load-success
  (fn [db [_ data]]
    (assoc db :meals-plans/meals (->> data
                                      (map (fn [meal-plan]
                                             (-> meal-plan
                                                 (update :date (fn [date] (.toDate date)))
                                                 (update :type (fn [t] (keyword "meal-type" t)))
                                                 (update :recipe recipe/->recipe))))))))

(reg-event-fx :meals/load-failure
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/error "Fehler: Essen nicht geladen."
           :meals-plans/meals [])
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))
