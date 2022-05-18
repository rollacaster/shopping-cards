(ns tech.thomas-sojka.shopping-cards.main.meal-plan-details.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn meal-plan-details []
  (fn []
    (let [{{:keys [id]} :recipe} @(subscribe [:recipe-details/meal])
          {:recipe/keys [name link image] :as recipe} @(subscribe [:recipes/details id])]
      [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
       [:div.flex.justify-between.items
        [:a.link.near-black.mb3.mb0-ns.db
         {:href link :target "_blank" :referer "norel noopener"
          :class (when-not (empty? link) "underline")}
         [:h1.mv0 name]]
        [:button.pv2.br3.bg-orange-200.bn.shadow-2.self-start
         {:on-click #(dispatch [:main/remove-meal])}
         [c/icon {:class "dark-gray h2"} :trash-can]]]
       [:div.flex.justify-between.flex-wrap.flex-column
        [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100
         [:img.w5.br3.ba.b--orange-300 {:src image}]]
        [:div
         [:h2.fw6.mb3 "Zutaten"]
         [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
          (map
           (fn [{:cooked-with/keys [id ingredient amount-desc]}]
             [:li.mb2.f4 {:key id} (str amount-desc " " (:ingredient/name ingredient))])
           (:cooked-with/_recipe recipe))]]]
       (when-not (empty? link)
         [:div.flex.justify-center
          [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
           {:href link}
           "Rezept anzeigen"]])])))
