(ns tech.thomas-sojka.shopping-cards.edit-recipes.core
  (:require [tech.thomas-sojka.shopping-cards.view :as core]
            [tech.thomas-sojka.shopping-cards.edit-recipes.views :as views]))

(defmethod core/content :view/edit-recipes [] [views/recipe-editing])
(defmethod core/title :view/edit-recipes [] "Rezepte bearbeiten")
