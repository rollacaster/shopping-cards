(ns tech.thomas-sojka.shopping-cards.views.shopping-list
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
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
    :reagent-render (fn [match]
                      (let [entries @(subscribe [:shopping-entries])]
                        [:ul.list.mv0.pa0
                         (map-indexed
                          (fn [idx {:shopping-item/keys [ingredient-id content status] :as entry}]
                            ^{:key ingredient-id}
                            [item-select {:i idx
                                          :entry entry
                                          :selected? (= status :done)
                                          :on-change (fn [selected?]
                                                       (dispatch [:shopping-item/update (assoc entry :shopping-item/status (if selected? :done :open))]))}
                             content])
                          entries)]))}))
