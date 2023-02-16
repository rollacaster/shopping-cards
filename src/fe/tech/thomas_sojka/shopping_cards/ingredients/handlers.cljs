(ns tech.thomas-sojka.shopping-cards.ingredients.handlers
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx :ingredients/add
  (fn [_ [_ ingredient]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "ingredients"
                           :data (assoc ingredient :id new-id)
                           :on-success [:ingredients/add]
                           :on-failure [:ingredients/add-failure]}})))

(reg-event-fx
  :ingredients/add-success
  (fn [{:keys [db]}]
    {:db (assoc db :ingredient-details/meal nil)
     :app/push-state [:route/main]}))

(reg-event-fx
 :ingredients/add-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx :ingredients/remove
  (fn [_ [_ ingredient-id]]
    {:firestore/remove-doc {:path "ingredients"
                            :key ingredient-id
                            :on-success [:ingredients/remove-success]
                            :on-failure [:ingredients/remove-failure]}}))

(reg-event-fx :ingredients/remove-success
  (fn []
    {:app/push-state [:route/ingredient]}))

(reg-event-fx :ingredients/remove-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Löschen fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))
