(ns tech.thomas-sojka.shopping-cards.views.shopping-list
  (:require [clojure.string :as str]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.components :as c]
            [tech.thomas-sojka.shopping-cards.dev-utils :as dev-utils]))

(defn item-select [{:keys [i id selected? amount on-change]} children]
  [:li.mh5-ns.ph4
   {:class (if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")}
   [:label.flex.items-center.pointer.f4 {:for id}
    [:div.flex.justify-between.w-100
     [:div.flex.items-center.pv3
      [:input.pointer.mr3.w2.h2
       {:id id :type "checkbox"
        :checked selected? :on-change
        (fn [e] (on-change ^js e.target.checked))}]
      children]
     [:div.flex.items-center.
      {:style {:gap 8}}
      [:button.bn.shadow-1.h2.w2.br1
       {:on-click #(dispatch [:shopping-items/decrease-amount id])
        :class (str/join " "
                         [(if (= (mod i 2) 0) "bg-orange-300 gray-700" "bg-gray-600 white")
                          (when (= amount 1) "o-50")])}
       "-"]
      [:div.w2.tc.fw6.f2 amount]
      [:button.bn.shadow-1.h2.w2.br1
       {:on-click #(dispatch [:shopping-items/increase-amount id])
        :class (str/join " "
                         [(if (= (mod i 2) 0) "bg-orange-300 gray-700" "bg-gray-600 white")
                          (when (= amount 9) "o-50")])}"+"]]]]])

(defn main []
  (r/create-class
   {:display-name "shopping-list"
    :component-will-unmount (fn []
                              (when-not @dev-utils/restarting
                                 (dispatch [:shopping-items/archive])))
    :reagent-render (fn []
                      (let [entries @(subscribe [:shopping-entries])]
                        [:<>
                         [:ul.list.mv0.pt0.pl0
                          {:style {:padding-bottom 50}}
                          (map-indexed
                           (fn [idx {:shopping-item/keys [id ingredient-id content status amount] :as entry}]
                             ^{:key ingredient-id}
                             [item-select {:i idx
                                           :id id
                                           :entry entry
                                           :amount amount
                                           :selected? (= status :done)
                                           :on-change (fn [selected?]
                                                        (dispatch [:shopping-item/update (assoc entry :shopping-item/status (if selected? :done :open))]))}
                              content])
                           entries)]
                         [c/add-button {:href (rfe/href :route/add-item)}]]))}))
