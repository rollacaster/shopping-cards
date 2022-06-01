(ns tech.thomas-sojka.shopping-cards.recipes.recipe.core
  (:require [tech.thomas-sojka.shopping-cards.recipes.recipe.subs]
            [tech.thomas-sojka.shopping-cards.recipes.recipe.views :as views]
            [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/recipe [_ match] [views/recipe-editing match])
(defmethod core/title :view/recipe [] "Rezept bearbeiten")
