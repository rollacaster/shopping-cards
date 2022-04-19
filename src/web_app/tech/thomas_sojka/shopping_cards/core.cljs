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
            [tech.thomas-sojka.shopping-cards.main.core]
            [tech.thomas-sojka.shopping-cards.edit-recipes.core]))

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
     :view :view/select-dinner}]
   ["/edit-recipes"
    {:name :route/edit-recipes
     :view :view/edit-recipes}]
   ["/edit-recipe/:recipe-id/new-ingredient"
    {:name :route/edit-recipe-add-ingredient
     :view :view/edit-recipe-add-ingredient
     :parameters {:path {:recipe-id string?}}}]
   ["/edit-recipe/:recipe-id"
    {:name :route/edit-recipe
     :view :view/edit-recipe
     :parameters {:path {:recipe-id string?}}}]])

(defn init! []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m]
     (dispatch [:app/navigate m]))
   {:use-fragment true})
  (dom/render [app] (.getElementById js/document "app"))
  (dispatch [:main/load-recipes])
  (dispatch [:main/init-meal-plans (js/Date.)]))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (clear-subscription-cache!)
  (init!))

(defonce start-up (do (dispatch-sync [:app/initialise (.getFullYear (js/Date.))]) true))

(init!)
