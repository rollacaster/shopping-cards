(ns tech.thomas-sojka.shopping-cards.main.core
  (:require [tech.thomas-sojka.shopping-cards.main.events]
            [tech.thomas-sojka.shopping-cards.main.subs]
            [tech.thomas-sojka.shopping-cards.main.views :as views]
            [tech.thomas-sojka.shopping-cards.main.meal-plan-details.core]
            [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.core]
            [tech.thomas-sojka.shopping-cards.main.select-lunch.core]
            [tech.thomas-sojka.shopping-cards.main.select-dinner.core]
            [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/main [] [views/meal-plan])
(defmethod core/title :view/main [] "Essensplan")
