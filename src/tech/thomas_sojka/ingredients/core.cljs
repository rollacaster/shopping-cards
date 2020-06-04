(ns tech.thomas-sojka.ingredients.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))

(defn app []
  [:div "Hi"])

(dom/render [app] (.getElementById js/document "app"))

