(ns tech.thomas-sojka.shopping-cards.main.select-lunch.core
  (:require
   [tech.thomas-sojka.shopping-cards.main.select-lunch.views :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/select-lunch [] [views/select-lunch])
(defmethod core/title :view/select-lunch [] "Mittag auswählen")
