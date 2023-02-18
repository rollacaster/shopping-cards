(ns tech.thomas-sojka.shopping-cards.core
  (:require [day8.re-frame.http-fx]
            [re-frame.core :refer [clear-subscription-cache! dispatch
                                   dispatch-sync subscribe]]
            [reagent.dom :as dom]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.app]
            [tech.thomas-sojka.shopping-cards.bank-holidays]
            [tech.thomas-sojka.shopping-cards.firestore]
            [tech.thomas-sojka.shopping-cards.meal-plans]
            [tech.thomas-sojka.shopping-cards.recipes]
            [tech.thomas-sojka.shopping-cards.shopping-list]
            [tech.thomas-sojka.shopping-cards.views.deselect-ingredients :as deselect-ingredients]
            [tech.thomas-sojka.shopping-cards.views.finish :as finish]
            [tech.thomas-sojka.shopping-cards.views.ingredient-add :as ingredient-add]
            [tech.thomas-sojka.shopping-cards.views.ingredients :as ingredients]
            [tech.thomas-sojka.shopping-cards.views.login :as login]
            [tech.thomas-sojka.shopping-cards.views.main :as main]
            [tech.thomas-sojka.shopping-cards.views.meal-plan :as meal-plan]
            [tech.thomas-sojka.shopping-cards.views.meal-plan-details :as meal-plan-details]
            [tech.thomas-sojka.shopping-cards.views.recipe-add :as recipe-add]
            [tech.thomas-sojka.shopping-cards.views.recipe-details :as recipe-details]
            [tech.thomas-sojka.shopping-cards.views.recipes :as recipes]
            [tech.thomas-sojka.shopping-cards.views.select-dinner :as select-dinner]
            [tech.thomas-sojka.shopping-cards.views.select-lunch :as select-lunch]))

(def routes
  [["/" {:name :route/main
         :title "Essensplan"
         :view meal-plan/main}]
   ["/login" {:name :route/login
              :title "Login"
              :view login/main}]
   ["/meal-plan" {:name :route/meal-plan
                  :title "Essensplan"
                  :view meal-plan/main}]
   ["/meal-plan-details/:meal-id" {:name :route/meal-plan-details
                          :title "Rezept"
                          :view meal-plan-details/base
                          :parameters {:path {:meal-id string?}}}]
   ["/deselect-ingredients" {:name :route/deselect-ingredients
                             :title "Zutaten auswählen"
                             :view deselect-ingredients/main}]
   ["/finish/:card-id" {:name :route/finish
                        :title "Einkaufszettel erstellt"
                        :view finish/main
                        :parameters {:path {:card-id string?}}}]
   ["/select-lunch" {:name :route/select-lunch
                     :title "Mittag auswählen"
                     :view select-lunch/main
                     :parameters {:query {:type keyword? :date string?}}}]
   ["/select-dinner" {:name :route/select-dinner
                      :title "Abendessen auswählen"
                      :view select-dinner/main
                      :parameters {:query {:type keyword? :date string?}}}]
   ["/recipes" {:name :route/edit-recipes
                :title "Rezepte bearbeiten"
                :view recipes/main}]
   ["/recipe-add" {:name :route/new-recipe
                   :title "Neues Rezept"
                   :view recipe-add/main}]
   ["/recipes/:recipe-id" {:name :route/edit-recipe
                           :title "Rezept bearbeiten"
                           :view recipe-details/main
                           :parameters {:path {:recipe-id string?}}}]
   ["/ingredients" {:name :route/ingredients
                    :title "Zutaten"
                    :view ingredients/main}]
   ["/ingredient-add" {:name :route/new-ingredient
                       :title "Neue Zutat"
                       :view ingredient-add/main}]])

(defn init!
  ([]
   (init! {:container (.getElementById js/document "app")}))
  ([{:keys [container]}]
   (rfe/start!
    (rf/router routes {:data {:coercion rss/coercion}})
    (fn [m]
      (dispatch [:app/navigate m]))
    {:use-fragment true})
   (when (empty? @(subscribe [:recipes]))
     (dispatch-sync [:app/initialise (js/Date.)]))
   (dom/render [main/app] container)))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (clear-subscription-cache!)
  (init!))
