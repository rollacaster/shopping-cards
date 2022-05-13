(ns tech.thomas-sojka.shopping-cards.ingredients.core
  (:require
   [tech.thomas-sojka.shopping-cards.ingredients.event]
   [tech.thomas-sojka.shopping-cards.ingredients.subs]
   [tech.thomas-sojka.shopping-cards.ingredients.views :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/ingredients [] [views/ingredients])
(defmethod core/title :view/ingredients [] "Zutaten")
