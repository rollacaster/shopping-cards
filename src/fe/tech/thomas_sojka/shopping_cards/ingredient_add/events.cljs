(ns tech.thomas-sojka.shopping-cards.ingredient-add.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :ingredient-add/success
  (fn []
    {:app/push-state [:route/ingredients]}))

(reg-event-fx :ingredient-add/failure
 (fn [{:keys [db]} _]
   {:db (assoc db :app/error "Fehler: Fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-db :ingredient/success-categories-load
  (fn [db [_ categories]]
    (assoc db :categories (map first categories))))
