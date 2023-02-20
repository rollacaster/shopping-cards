(ns tech.thomas-sojka.shopping-cards.views.shopping-card
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn item-select [{:keys [i id selected? on-change]} children]
  [c/ingredient {:i i :id id :class (when selected? "strike")}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox"
      :checked selected? :on-change on-change}]
    children]])

(defn main [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [card-id]} path
        {:shopping-card/keys [entries]} @(subscribe [:shopping-list/shopping-card card-id])]
    [:ul.list.mv0.pa0
     (map-indexed
      (fn [idx {:shopping-entry/keys [ingredient-id item status]}]
        [:li {:key ingredient-id}
         [item-select {:i idx
                       :selected? (= status :done)
                       :on-change #()}
          item]])
      entries)]))
