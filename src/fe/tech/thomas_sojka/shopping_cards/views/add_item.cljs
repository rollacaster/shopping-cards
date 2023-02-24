(ns tech.thomas-sojka.shopping-cards.views.add-item
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

(defn main []
  (let [filter-value (r/atom "")]
    (fn []
      (let [ingredients @(subscribe [:ingredients])]
        [:div.bg-gray-200
         [:div.w-100
          [:input.pv3.bn.pl4.w-100.border-box.bg-orange-100.f4
           {:placeholder "Suche ..."
            :value @filter-value
            :on-change (fn [event] (reset! filter-value ^js (.-target.value event)))}]]
         [:ul.pl0.mv0
          (doall
           (->> ingredients
                (sort-by :ingredient/name)
                (filter (fn [{:keys [ingredient/name]}]
                          (str/includes? (str/lower-case name)
                                         (str/lower-case @filter-value))))
                (map-indexed
                 (fn [i {:ingredient/keys [id name]}]
                   ^{:key id}
                   [:li.mh5-ns.w-100
                    [:button.w-100.bg-transparent.bn.ph4.pv3.pointer.f4.tl
                     {:class [(if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")]
                      :on-click #(dispatch [:shopping-item/add{:ingredient-id id
                                                               :content name}])}
                     name]]))))] ]))))
