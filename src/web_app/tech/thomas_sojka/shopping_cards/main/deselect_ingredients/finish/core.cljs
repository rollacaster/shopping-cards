(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.finish.core
  (:require [tech.thomas-sojka.shopping-cards.view :as core]))

(defn finish [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [card-id]} path]
    [:div.flex.justify-center.pv5
     [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
      {:href (str "https://trello.com/c/" card-id)}
      "In Trello anzeigen"]]))

(defmethod core/content :view/finish [_ match] [finish match])
(defmethod core/title :view/finish [] "Einkaufszettel erstellt")

