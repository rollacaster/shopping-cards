(ns tech.thomas-sojka.shopping-cards.core
  (:require [re-frame.core
             :refer
             [clear-subscription-cache! dispatch dispatch-sync]]
            [reagent.dom :as dom]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.view :refer [app]]
            [day8.re-frame.http-fx]
            [tech.thomas-sojka.shopping-cards.events]
            [tech.thomas-sojka.shopping-cards.subs]
            [tech.thomas-sojka.shopping-cards.main.core]))

(def routes
  [[["/" {:name :route/main
          :view :view/main}]
   ["/meal-plan" {:name :route/meal-plan
                  :view :view/main}]]
   ["/meal-plan-details"
    {:name :route/meal-plan-details
     :view :view/meal-plan-details}]
   ["/deselect-ingredients" {:name :route/deselect-ingredients
                             :view :view/deselect-ingredients}]
   ["/add-ingredients" {:name :route/add-ingredients
                        :view :view/add-ingredients}]
   ["/finish/:card-id" {:name :route/finish
                        :view :view/finish
                        :parameters {:path {:card-id string?}}}]
   ["/select-lunch"
    {:name :route/select-lunch
     :view :view/select-lunch}]
   ["/select-dinner"
    {:name :route/select-dinner
     :view :view/select-dinner}]])

(defn init! []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m]
     (dispatch [:app/navigate m]))
   {:use-fragment true})
  (dom/render [app] (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (clear-subscription-cache!)
  (init!))

(defonce start-up (do (dispatch-sync [:app/initialise (.getFullYear (js/Date.))]) true))

(init!)
