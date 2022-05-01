(ns tech.thomas-sojka.shopping-cards.recipes.core
  (:require [tech.thomas-sojka.shopping-cards.view :as core]
            [tech.thomas-sojka.shopping-cards.recipes.views :as views]
            [tech.thomas-sojka.shopping-cards.recipes.events]
            [tech.thomas-sojka.shopping-cards.recipes.subs]))

(defmethod core/content :view/recipes [] [views/recipes-editing])
(defmethod core/title :view/recipes [] "Rezepte bearbeiten")

(defmethod core/content :view/recipe [_ match] [views/recipe-editing match])
(defmethod core/title :view/recipe [] "Rezept bearbeiten")
