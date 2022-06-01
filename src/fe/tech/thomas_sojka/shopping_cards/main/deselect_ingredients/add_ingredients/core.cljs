(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.core
  (:require
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.views
    :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/add-ingredients [] [views/add-ingredients])
(defmethod core/title :view/add-ingredients [] "Zutaten hinzufügen")
