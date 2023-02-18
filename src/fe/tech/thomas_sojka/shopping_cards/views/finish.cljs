(ns tech.thomas-sojka.shopping-cards.views.finish)

(defn main [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [card-id]} path]
    [:div.flex.justify-center.pv5
     [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
      {:href (str "https://trello.com/c/" card-id)}
      "In Trello anzeigen"]]))
