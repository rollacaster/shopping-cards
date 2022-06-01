(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.finish.core
  (:require
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.finish.views
    :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/finish [_ match] [views/finish match])
(defmethod core/title :view/finish [] "Einkaufszettel erstellt")
