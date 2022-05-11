(ns tech.thomas-sojka.shopping-cards.recipe-add.core
  (:require
   [tech.thomas-sojka.shopping-cards.recipe-add.events]
   [tech.thomas-sojka.shopping-cards.recipe-add.views :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/new-recipe [] [views/new-recipe])
(defmethod core/title :view/new-recipe [] "Neues Rezept")
