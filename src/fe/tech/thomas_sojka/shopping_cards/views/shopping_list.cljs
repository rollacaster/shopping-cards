(ns tech.thomas-sojka.shopping-cards.views.shopping-list
  (:require [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn item-select [{:keys [i id selected? on-change]} children]
  [c/ingredient {:i i :id id :class (when selected? "strike")}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox"
      :checked selected? :on-change
      (fn [e] (on-change ^js e.target.checked))}]
    children]])

(defn main []
  (let [entries @(subscribe [:shopping-entries])]
    [:ul.list.mv0.pa0
     (map-indexed
      (fn [idx {:shopping-entry/keys [ingredient-id item status] :as entry}]
        [:li {:key ingredient-id}
         [item-select {:i idx
                       :entry entry
                       :selected? (= status :done)
                       :on-change (fn [selected?]
                                    (dispatch [:shopping-entry/update (assoc entry :shopping-entry/status (if selected? :done :open))]))}
          item]])
      entries)]))
