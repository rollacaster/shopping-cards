(ns tech.thomas-sojka.shopping-cards.views.add-item
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn main []
  (let [filter-value (r/atom "")]
    (fn []
      (let [ingredients @(subscribe [:shopping-item/possible-ingredients])
            filtered-ingredients (->> ingredients
                                      (sort-by :ingredient/name)
                                      (filter (fn [{:keys [ingredient/name]}]
                                                (str/includes? (str/lower-case name)
                                                               (str/lower-case @filter-value)))))]
        [:div.bg-gray-200
         [c/search-filter {:value @filter-value
                           :on-change (fn [event] (reset! filter-value ^js (.-target.value event)))}]
         [:ul.pl0.mv0
          (if (zero? (count filtered-ingredients))
            [:li.mh5-ns.w-100
             [:a.link.w-100.bg-transparent.bn.ph4.pv3.pointer.f4.tl.i.flex
              {:class ["bg-gray-600 white"]
               :href (rfe/href :route/new-ingredient {:ingredient-name @filter-value})}
              [:span.mr2
               [c/icon {:class "br-100 bg-orange-400"
                        :style {:width "1.5rem" :height "1.5rem"}}
                :add]]
              [:span (str @filter-value " hinzufügen")]]]
            (doall
             (map-indexed
              (fn [i {:ingredient/keys [id name]}]
                ^{:key id}
                [:li.mh5-ns.w-100
                 [:button.w-100.bg-transparent.bn.ph4.pv3.pointer.f4.tl
                  {:class [(if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")]
                   :on-click #(dispatch [:shopping-item/add{:ingredient-id id
                                                            :content name}])}
                  name]])
              filtered-ingredients)))] ]))))
