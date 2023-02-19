(ns tech.thomas-sojka.shopping-cards.meal-plans
  (:require ["date-fns" :refer (addDays startOfDay isAfter addDays isEqual)]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub reg-sub]]
            [tech.thomas-sojka.shopping-cards.recipes :as recipe]))

(reg-event-fx :meal/add
  (fn [_ [_ meal]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "meal-plans"
                           :data (-> meal
                                     (assoc :id new-id)
                                     (update :recipe dissoc :ingredients))
                           :on-success [:meal/add-success]
                           :on-failure [:meal/add-failure]}})))

(reg-event-fx :meal/add-success
  (fn []
    {:app/push-state [:route/main]}))

(reg-event-fx :meal/add-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :meal/remove
  (fn [_ [_ meal-plan-id]]
    {:app/push-state [:route/meal-plan]
     :firestore/remove-doc {:path "meal-plans"
                            :key meal-plan-id
                            :on-failure [:meal/remove-failure]}}))

(reg-event-fx :meal/remove-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Löschen fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(defn- meal-plans-loaded-for-today? [db today]
  (->> (:meals db)
       (map #(:date %))
       (some #(= today %))))

(reg-event-fx :meals/load (fn [{:keys [db]} [_ today]]
                            (if (meal-plans-loaded-for-today? db today)
                              {:db db}
                              {:firestore/snapshot {:path "meal-plans"
                                                    :on-success [:meal/load-success]
                                                    :on-failure [:meal/load-failure]}})))

(defn- ->meal-plan [firestore-meal-plan]
  (-> firestore-meal-plan
      (update :date (fn [date] (.toDate date)))
      (update :type (fn [t] (keyword "meal-type" t)))
      (update :recipe recipe/->recipe)))

(reg-event-db :meal/load-success
  (fn [db [_ data]]
    (assoc db :meals (map ->meal-plan data))))

(reg-event-fx :meal/load-failure
  (fn [{:keys [db]}]
    {:db
     (assoc db
            :app/error "Fehler: Essen nicht geladen."
            :meals [])
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-sub :meals
  (fn [db _]
    (:meals db)))

(defn group-meal-plans [meal-plans]
  (->> meal-plans
       (group-by :date)
       (map #(hash-map (first %) (->> (second %)
                                      (group-by :type)
                                      (map (fn [[type [recipe]]] (hash-map type recipe)))
                                      (apply merge))))
       (apply merge)))

(reg-sub :meal-plans/weekly
  :<- [:meals]
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

(reg-sub :meals-without-shopping-list
  :<- [:meals]
  (fn [meals-plans]
    (filter
     (fn [{:keys [date shopping-list]}]
       (and
        (not shopping-list)
        (or (isAfter (startOfDay date) (startOfDay (js/Date.)))
            (isEqual (startOfDay date) (startOfDay (js/Date.))))))
     meals-plans)))

;; TODO Rearrange data?
(defn- attach-ingredients [recipes meal]
  (assoc meal :recipe (first
                       (get (->> recipes (group-by :id))
                            (:id (:recipe meal))))))

(reg-sub :meal/details
  :<- [:recipes]
  :<- [:meals]
  (fn [[recipes meals] [_ meal-id]]
    (some
     (fn [{:keys [id] :as meal-plan}] (when (= id meal-id)
                                       (attach-ingredients recipes meal-plan)))
     meals)))
