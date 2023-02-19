(ns tech.thomas-sojka.shopping-cards.router
  (:require [reagent.core :as r]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.views.deselect-ingredients :as deselect-ingredients]
            [tech.thomas-sojka.shopping-cards.views.finish :as finish]
            [tech.thomas-sojka.shopping-cards.views.ingredient-add :as ingredient-add]
            [tech.thomas-sojka.shopping-cards.views.ingredients :as ingredients]
            [tech.thomas-sojka.shopping-cards.views.login :as login]
            [tech.thomas-sojka.shopping-cards.views.meal-plan :as meal-plan]
            [tech.thomas-sojka.shopping-cards.views.meal-plan-details :as meal-plan-details]
            [tech.thomas-sojka.shopping-cards.views.recipe-add :as recipe-add]
            [tech.thomas-sojka.shopping-cards.views.recipe-details :as recipe-details]
            [tech.thomas-sojka.shopping-cards.views.recipes :as recipes]
            [tech.thomas-sojka.shopping-cards.views.select-dinner :as select-dinner]
            [tech.thomas-sojka.shopping-cards.views.select-lunch :as select-lunch]))

(defonce match (r/atom nil))

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
(defn init []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m]
     ;; TODO dispatch params
     (reset! match m))
   {:use-fragment true})
  match)
