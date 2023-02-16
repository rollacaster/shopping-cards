(ns tech.thomas-sojka.shopping-cards.meal-plans.handlers
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx :meal-plans/add
  (fn [{:keys [db]} [_ recipe]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "meal-plans"
                           :data (assoc (:recipe-details/meal db)
                                        :recipe recipe :id new-id)
                           :on-success [:meal-plans/add]
                           :on-failure [:meal-plans/add-failure]}})))

(reg-event-fx
  :meal-plans/add-success
  (fn [{:keys [db]}]
    {:db (assoc db :recipe-details/meal nil)
     :app/push-state [:route/main]}))

(reg-event-fx :meal-plans/add-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx :meal-plans/remove
  (fn [_ [_ meal-plan-id]]
    {:firestore/remove-doc {:path "meal-plans"
                            :key meal-plan-id
                            :on-success [:meal-plans/remove-success]
                            :on-failure [:meal-plans/remove-failure]}}))

(reg-event-fx :meal-plans/remove-success
  (fn []
    {:app/push-state [:route/meal-plan]}))

(reg-event-fx :meal-plans/remove-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Löschen fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))
