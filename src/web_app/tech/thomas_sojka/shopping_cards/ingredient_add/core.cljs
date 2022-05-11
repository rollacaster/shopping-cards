(ns tech.thomas-sojka.shopping-cards.ingredient-add.core
  (:require
   [tech.thomas-sojka.shopping-cards.ingredient-add.events]
   [tech.thomas-sojka.shopping-cards.ingredient-add.subs]
   [tech.thomas-sojka.shopping-cards.ingredient-add.views :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/new-ingredient [] [views/new-ingredient])
(defmethod core/title :view/new-ingredient [] "Neues Rezept")
