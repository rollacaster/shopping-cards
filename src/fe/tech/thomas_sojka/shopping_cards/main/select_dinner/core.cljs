(ns tech.thomas-sojka.shopping-cards.main.select-dinner.core
  (:require
   [tech.thomas-sojka.shopping-cards.main.select-dinner.views :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/select-dinner [] [views/select-dinner])
(defmethod core/title :view/select-dinner [] "Abendessen auswählen")
