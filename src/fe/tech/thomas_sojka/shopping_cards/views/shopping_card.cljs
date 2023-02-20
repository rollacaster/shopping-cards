(ns tech.thomas-sojka.shopping-cards.views.shopping-card
  (:require [re-frame.core :refer [subscribe]]))

(defn main [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [card-id]} path
        shopping-card @(subscribe [:shopping-list/shopping-card card-id])]
    [:div (prn-str shopping-card)]))
