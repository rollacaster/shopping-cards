(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.core
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [tech.thomas-sojka.shopping-cards.components :refer [icon]]
   [tech.thomas-sojka.shopping-cards.main.components :as c]
   [tech.thomas-sojka.shopping-cards.view :as core]
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.events]
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.subs]))

(defn add-ingredients []
  (let [ingredients @(subscribe [:extra-ingredients/addable-ingredients])
        ingredient-filter @(subscribe [:extra-ingredients/filter])]
    [:div.ph5-ns.flex.flex-column.h-100
     [:div.ph2.pt3
      [:input.h2.br3.ba.b--gray.ph2 {:value ingredient-filter
                                     :autoFocus true
                                     :on-change (fn [e] (dispatch [:extra-ingredients/filter-ingredients ^js (.-target.value e)]))
                                     :placeholder "Suche..."}]]
     [:ul.list.pl0.mv0.pb6
      (map-indexed (fn [i {:ingredient/keys [id name]}]
                     [:button.bn.pa0.w-100.ma0.dib
                      {:key id
                       :on-click #(dispatch [:extra-ingredients/add id name])}
                      [c/ingredient
                       {:i i
                        :id id
                        :selected? false}
                       [:div.flex.w-100
                        [:div.mr3.flex.items-center
                         [icon {:style {:width 20}} :add]]
                        name]]])
                   ingredients)]
     (when (seq [])
       [c/footer {:on-click #(dispatch [:add-ingredients []])}])]))

(defmethod core/content :view/add-ingredients [] [add-ingredients])
(defmethod core/title :view/add-ingredients [] "Zutaten hinzufügen")
