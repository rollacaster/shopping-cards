(ns tech.thomas-sojka.shopping-cards.views.deselect-ingredients
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn ingredient-select [{:keys [i id selected? on-change]} children]
  [c/ingredient {:i i :id id}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox" :checked selected? :on-change on-change}]
    children]])

(defn toggle-map [m k]
  ((if (m k) disj conj) m k))

(defn main []
  (let [ingredients @(subscribe [:shopping-list/possible-ingredients])
        selected-ingredients (r/atom (set (map first ingredients)))]
    (fn []
      (let [loading @(subscribe [:app/loading])
            ingredients @(subscribe [:shopping-list/possible-ingredients])]
        [:<>
         [:ul.list.pl0.mv0.pb6
          (doall
           (map-indexed (fn [i [id content]]
                          [ingredient-select
                           (let [selected? (contains? @selected-ingredients id)]
                             {:key id
                              :i i
                              :id id
                              :selected? selected?
                              :on-change
                              #(swap! selected-ingredients toggle-map id)})
                           content])
                        ingredients))]
         [:div.fixed.bottom-0.w-100.z-2
          [c/footer {:on-click #(dispatch [:shopping-card/create ingredients @selected-ingredients])
                     :app/loading loading}]]]))))
