(ns tech.thomas-sojka.shopping-cards.views.shopping-list
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.components :as c]
            [tech.thomas-sojka.shopping-cards.dev-utils :as dev-utils]))

(defn item-select [{:keys [i id selected? on-change]} children]
  [c/ingredient {:i i :id id :class (when selected? "strike")}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox"
      :checked selected? :on-change
      (fn [e] (on-change ^js e.target.checked))}]
    children]])

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
                           (fn [idx {:shopping-item/keys [ingredient-id content status] :as entry}]
                             ^{:key ingredient-id}
                             [item-select {:i idx
                                           :entry entry
                                           :selected? (= status :done)
                                           :on-change (fn [selected?]
                                                        (dispatch [:shopping-item/update (assoc entry :shopping-item/status (if selected? :done :open))]))}
                              content])
                           entries)]
                         [:a.fixed.bottom-2.right-0.bg-orange-500.ma4.pa4.z-1.br-100.relative.shadow-5.bn
                          {:href (rfe/href :route/add-item)}
                          [:span.absolute.f1.white.lh-solid.flex.align-items.justify-center
                           {:style {:top "45%"
                                    :left "50%"
                                    :transform "translate(-50%,-50%)"}}
                           "+"]]]))}))
