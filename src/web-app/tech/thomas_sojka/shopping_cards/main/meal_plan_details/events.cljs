(ns tech.thomas-sojka.shopping-cards.main.meal-plan-details.events
  (:require
   [ajax.core :as ajax]
   [cljs.reader :refer [read-string]]
   [re-frame.core :refer [reg-event-db reg-event-fx ]]))

(reg-event-fx
 :recipe-details/show-meal-details
 (fn [{:keys [db]} [_ meal]]
   {:app/push-state [:route/meal-plan-details]
    :db (assoc db :recipe-details/meal meal)
    :http-xhrio {:method :get
                 :uri (str "/recipes/" (:id (:recipe meal)) "/ingredients")
                 :response-format (ajax/raw-response-format)
                 :on-success [:recipe-details/success-load-ingredients-for-recipe]
                 :on-failure [:recipe-details/failure-load-ingredients-for-recipe]}}))

(reg-event-db
 :recipe-details/success-load-ingredients-for-recipe
 (fn [db [_ data]]
   (assoc db :recipe-details/ingredients (read-string data))))

(reg-event-db
 :recipe-details/failure-load-ingredients-for-recipe
 (fn [db _]
   (assoc db :recipe-details/ingredients :ERROR)))
