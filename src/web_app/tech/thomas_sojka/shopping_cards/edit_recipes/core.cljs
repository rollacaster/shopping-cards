(ns tech.thomas-sojka.shopping-cards.edit-recipes.core
  (:require [tech.thomas-sojka.shopping-cards.view :as core]
            [tech.thomas-sojka.shopping-cards.edit-recipes.views :as views]
            [tech.thomas-sojka.shopping-cards.edit-recipes.events]
            [tech.thomas-sojka.shopping-cards.edit-recipes.subs]))

(defmethod core/content :view/edit-recipes [] [views/recipes-editing])
(defmethod core/title :view/edit-recipes [] "Rezepte bearbeiten")

(defmethod core/content :view/edit-recipe [_ match] [views/recipe-editing match])
(defmethod core/title :view/edit-recipe [] "Rezept bearbeiten")

(defmethod core/content :view/edit-recipe-add-ingredient [_ match]
  [views/add-ingredient match])
(defmethod core/title :view/edit-recipe-add-ingredient [] "Zutat hinzufügen")
