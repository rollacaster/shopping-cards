(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [ajax.core :as ajax]))

(reg-event-db
 :extra-ingredients/filter-ingredients
 (fn [db [_ filter]]
   (assoc db :extra-ingredients/filter filter)))

(reg-event-fx
 :extra-ingredients/show
 (fn [_ _]
   {:app/push-state [:route/add-ingredients]
    :app/scroll-to [0 0]
    :http-xhrio {:method :get
                 :uri "/ingredients"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:extra-ingredients/success-load-ingredients]
                 :on-failure [:extra-ingredients/success-failure-ingredients]}}))

(reg-event-db
 :extra-ingredients/success-load-ingredients
 (fn [db [_ ingredients]]
   (assoc db :extra-ingredients/ingredients ingredients)))

(reg-event-fx
 :extra-ingredients/add
 (fn [{:keys [db]} [_ id name]]
   {:db (-> db
            (update :shopping-card/ingredients conj [id name])
            (update :shopping-card/selected-ingredient-ids conj id)
            (assoc :extra-ingredients/filter ""))
    :app/push-state [:route/deselect-ingredients]}))

