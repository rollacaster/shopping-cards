(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.core
  (:require [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.main.components :as c]
            [tech.thomas-sojka.shopping-cards.view :as core]
            [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.core]
            [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.subs]
            [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.events]
            [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.finish.core]))

(defn ingredient-select [{:keys [i id selected? on-change]} children]
  [c/ingredient {:i i :id id}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox" :checked selected? :on-change on-change}]
    children]])

(defn deselect-ingredients []
  (let [selected-ingredients @(subscribe [:shopping-card/selected-ingredient-ids])
        ingredients @(subscribe [:shopping-card/ingredients])
        loading @(subscribe [:app/loading])
        meals-without-shopping-list @(subscribe [:main/meals-without-shopping-list])]
    [:<>
     [:ul.list.pl0.mv0.pb6
      (map-indexed (fn [i [id content]]
                     [ingredient-select
                      (let [selected? (contains? selected-ingredients id)]
                        {:key id
                         :i i
                         :id id
                         :selected? selected?
                         :on-change
                         #(dispatch [:shopping-card/toggle-selected-ingredients id])})
                      content])
                   ingredients)
      [:button.bn.bg-transparent.w-100.pa0
       {:on-click #(dispatch [:extra-ingredients/show])}
       [c/ingredient {:i (count ingredients) :id "add-ingredient" :class "ba b--dashed"}
        [:div.flex.items-center
         [:div.w2.flex.items-center.mr3
          [c/icon :add]]
         "Zutat hinzufügen"]]]]
     [:div.fixed.bottom-0.w-100.z-2
      [c/footer {:on-click #(dispatch [:shopping-card/create
                                       meals-without-shopping-list])
                 :app/loading loading}]]]))

(defmethod core/content :view/deselect-ingredients [] [deselect-ingredients])
(defmethod core/title :view/deselect-ingredients [] "Zutaten auswählen")
