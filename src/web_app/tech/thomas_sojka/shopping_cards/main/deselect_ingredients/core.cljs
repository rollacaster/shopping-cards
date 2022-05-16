(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.core
  (:require
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.events]
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.finish.core]
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.subs]
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.views
    :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/deselect-ingredients [] [views/deselect-ingredients])
(defmethod core/title :view/deselect-ingredients [] "Zutaten auswählen")
