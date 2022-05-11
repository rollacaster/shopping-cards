(ns tech.thomas-sojka.shopping-cards.recipes.core
  (:require [tech.thomas-sojka.shopping-cards.view :as core]
            [tech.thomas-sojka.shopping-cards.recipes.views :as views]
            [tech.thomas-sojka.shopping-cards.recipes.events]
            [tech.thomas-sojka.shopping-cards.recipes.subs]
            [tech.thomas-sojka.shopping-cards.recipes.recipe.core]))

(defmethod core/content :view/recipes [] [views/recipes-editing])
(defmethod core/title :view/recipes [] "Rezepte bearbeiten")
