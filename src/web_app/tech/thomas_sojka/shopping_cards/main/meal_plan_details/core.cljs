(ns tech.thomas-sojka.shopping-cards.main.meal-plan-details.core
  (:require
   [tech.thomas-sojka.shopping-cards.main.meal-plan-details.events]
   [tech.thomas-sojka.shopping-cards.main.meal-plan-details.subs]
   [tech.thomas-sojka.shopping-cards.main.meal-plan-details.views
    :as views]
   [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/meal-plan-details [] [views/meal-plan-details])
(defmethod core/title :view/meal-plan-details [] "Rezept")
