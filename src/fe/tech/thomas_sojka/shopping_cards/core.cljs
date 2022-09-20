(ns tech.thomas-sojka.shopping-cards.core
  (:require [day8.re-frame.http-fx]
            [re-frame.core
             :refer
             [clear-subscription-cache! dispatch dispatch-sync]]
            [reagent.dom :as dom]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.events]
            [tech.thomas-sojka.shopping-cards.fx]
            tech.thomas-sojka.shopping-cards.ingredient-add.core
            [tech.thomas-sojka.shopping-cards.ingredients.core]
            [tech.thomas-sojka.shopping-cards.main.core]
            [tech.thomas-sojka.shopping-cards.login]
            [tech.thomas-sojka.shopping-cards.recipe-add.core]
            [tech.thomas-sojka.shopping-cards.recipes.core]
            [tech.thomas-sojka.shopping-cards.subs]
            [tech.thomas-sojka.shopping-cards.view :refer [app]]))

(def routes
  [[["/" {:name :route/main
          :view :view/main}]
    ["/login" {:name :route/login
               :view :view/login}]
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
   ["/recipes"
    {:name :route/edit-recipes
     :view :view/recipes}]
   ["/recipe-add"
    {:name :route/new-recipe
     :view :view/new-recipe}]
   ["/recipes/:recipe-id"
    {:name :route/edit-recipe
     :view :view/recipe
     :parameters {:path {:recipe-id string?}}}]
   ["/ingredients"
    {:name :route/ingredients
     :view :view/ingredients}]
   ["/ingredient-add"
    {:name :route/new-ingredient
     :view :view/new-ingredient}]])

(defn init!
  ([]
   (init! {:container (.getElementById js/document "app")}))
  ([{:keys [container]}]
   (rfe/start!
    (rf/router routes {:data {:coercion rss/coercion}})
    (fn [m]
      (dispatch [:app/navigate m]))
    {:use-fragment true})
   (dispatch-sync [:app/initialise (js/Date.)])
   (dom/render [app] container)))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (clear-subscription-cache!)
  (init!))
