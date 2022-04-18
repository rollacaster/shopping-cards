(ns tech.thomas-sojka.shopping-cards.edit-recipes.events
  (:require
   [ajax.core :as ajax]
   [cljs.reader :refer [read-string]]
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx
 :edit-recipe/show-recipe
 (fn [_ [_ id]]
   {:app/push-state [:route/edit-recipe {:recipe-id id}]
    :dispatch [:edit-recipe/load-ingredients id]}))

(reg-event-fx
 :edit-recipe/load-ingredients
 (fn [_ [_ id]]
   {:http-xhrio {:method :get
                 :uri (str "/recipes/" id "/ingredients")
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-load-ingredients-for-recipe id]
                 :on-failure [:edit-recipe/failure-load-ingredients-for-recipe id]}}))

(reg-event-db
 :edit-recipe/success-load-ingredients-for-recipe
 (fn [db [_ id data]]
   (assoc-in db [:edit-recipe/ingredients id] (read-string data))))

(reg-event-db
 :edit-recipe/failure-load-ingredients-for-recipe
 (fn [db [_ id]]
   (assoc-in db [:edit-recipe/ingredients id] :ERROR)))
