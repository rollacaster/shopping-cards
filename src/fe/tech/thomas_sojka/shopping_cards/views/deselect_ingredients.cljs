(ns tech.thomas-sojka.shopping-cards.views.deselect-ingredients
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn item-select [{:keys [i id selected? on-change]} children]
  [c/ingredient {:i i :id id}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox" :checked selected? :on-change on-change}]
    children]])

(defn toggle-map [m k]
  ((if (m k) disj conj) m k))

(defn main []
  (let [ingredients @(subscribe [:shopping-item/possible-items])
        selected-items (r/atom (set (map first ingredients)))]
    (fn []
      (let [loading @(subscribe [:app/loading])
            items @(subscribe [:shopping-item/possible-items])]
        [:<>
         [:ul.list.pl0.mv0.pb6
          (doall
           (map-indexed (fn [i [id content]]
                          [item-select
                           (let [selected? (contains? @selected-items id)]
                             {:key id
                              :i i
                              :id id
                              :selected? selected?
                              :on-change
                              #(swap! selected-items toggle-map id)})
                           content])
                        items))]
         [:div.fixed.bottom-0.w-100.z-2
          [c/footer {:on-click #(dispatch [:shopping-item/create
                                           {:items items
                                            :selected-items @selected-items}])
                     :app/loading loading}]]]))))
