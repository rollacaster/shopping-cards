(ns tech.thomas-sojka.shopping-cards.views.meal-plan-details
  (:require [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]
            [tech.thomas-sojka.shopping-cards.ingredients-processing :as ingredients-processing]))

(defn base []
  (fn [match]
    (let [{:keys [path]} (:parameters match)
          {:keys [meal-id]} path
          {:keys [id recipe]} @(subscribe [:meal/details meal-id])
          {:keys [name link image]} recipe]
      [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
       [:div.flex.justify-between.items
        [:a.link.near-black.mb3.mb0-ns.db
         {:href link :target "_blank" :referer "norel noopener"
          :class (when-not (empty? link) "underline")}
         [:h1.mv0 name]]
        [:button.pv2.br3.bg-orange-200.bn.shadow-2.self-start
         {:on-click #(dispatch [:meal/remove id])}
         [c/icon {:class "dark-gray h2"} :trash-can]]]
       [:div.flex.justify-between.flex-wrap.flex-column
        [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100
         [:img.w5.br3.ba.b--orange-300 {:src image}]]
        [:div
         [:h2.fw6.mb3 "Zutaten"]
         [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
          (->> (:ingredients recipe)
               (sort-by
                (comp :ingredient/category second)
                (fn [category1 category2]
                  (< (.indexOf ingredients-processing/penny-order category1)
                     (.indexOf ingredients-processing/penny-order category2))))
               (map
                (fn [[{:cooked-with/keys [amount-desc]} {:ingredient/keys [id name]}]]
                  [:li.mb2.f4 {:key id} (str amount-desc " " name)])))]]]
       (when-not (empty? link)
         [:div.flex.justify-center
          [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
           {:href link}
           "Rezept anzeigen"]])])))
