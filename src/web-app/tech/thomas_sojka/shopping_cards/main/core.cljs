(ns tech.thomas-sojka.shopping-cards.main.core
  (:require [tech.thomas-sojka.shopping-cards.main.handlers]
            [tech.thomas-sojka.shopping-cards.main.subs]
            [tech.thomas-sojka.shopping-cards.main.views :as views]))

(def routes
  [["/" {:name :route/main
         :view views/meal-plan
         :title "Essensplan"}]
   ["/meal-plan" {:name :route/meal-plan
                  :view views/meal-plan
                  :title "Essensplan"}]])
