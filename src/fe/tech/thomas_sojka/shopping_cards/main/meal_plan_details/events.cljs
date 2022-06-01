(ns tech.thomas-sojka.shopping-cards.main.meal-plan-details.events
  (:require
   [re-frame.core :refer [reg-event-fx ]]))

(reg-event-fx
 :recipe-details/show-meal-details
 (fn [{:keys [db]} [_ meal]]
   {:app/push-state [:route/meal-plan-details]
    :db (assoc db :recipe-details/meal meal)
    :dispatch [:recipes/load (:id (:recipe meal))]}))
