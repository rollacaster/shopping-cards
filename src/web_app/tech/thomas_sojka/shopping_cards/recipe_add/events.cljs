(ns tech.thomas-sojka.shopping-cards.recipe-add.events
  (:require
   [ajax.core :as ajax]
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :recipe-add/new
  (fn [_ [_ recipe-url]]
    {:http-xhrio {:method :post
                  :uri "/recipe-add"
                  :params recipe-url
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:recipe-add/success]
                  :on-failure [:recipe-add/failure]}}))

(reg-event-db :recipe-add/success
  (fn []
    {:app/push-state [:route/edit-recipes]}))

(reg-event-fx :recipe-add/failure
 (fn [{:keys [db]} _]
   {:db (assoc db :app/error "Fehler: Fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))
