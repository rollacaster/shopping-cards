(ns tech.thomas-sojka.shopping-cards.recipes.handlers
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx :recipes/add
  (fn [_ [_ recipe]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "recipes"
                           :data (assoc recipe :id new-id)
                           :on-success [:recipes/add]
                           :on-failure [:recipes/add-failure]}})))

(reg-event-fx :recipes/add-success
  (fn [{:keys [db]}]
    {:db (assoc db :recipe-details/meal nil)
     :app/push-state [:route/main]}))

(reg-event-fx :recipes/add-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx :recipes/remove
  (fn [_ [_ recipe-id]]
    {:firestore/remove-doc {:path "recipes"
                            :key recipe-id
                            :on-success [:recipes/remove-success]
                            :on-failure [:recipes/remove-failure]}}))

(reg-event-fx :recipes/remove-success
  (fn []
    {:app/push-state [:route/recipe]}))

(reg-event-fx :recipes/remove-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Löschen fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))
