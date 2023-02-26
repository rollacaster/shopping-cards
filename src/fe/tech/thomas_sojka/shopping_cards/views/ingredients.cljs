(ns tech.thomas-sojka.shopping-cards.views.ingredients
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn main []
  (let [filter-value (r/atom "")]
    (fn []
      (let [ingredients @(subscribe [:ingredients])]
        [:div.bg-gray-200
         [c/search-filter {:value @filter-value
                           :on-change (fn [event] (reset! filter-value ^js (.-target.value event)))}]
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
                   [c/ingredient
                    {:i i
                     :id id
                     :selected? false}
                    [:div.flex.w-100
                     name]]))))]
         [:a.fixed.bottom-0.right-0.bg-orange-500.ma4.pa4.z-1.br-100.relative.shadow-5.bn
          {:href (rfe/href :route/new-ingredient)}
          [:span.absolute.f1.white.lh-solid.flex.align-items.justify-center
           {:style {:top "45%"
                    :left "50%"
                    :transform "translate(-50%,-50%)"}}
           "+"]]]))))
