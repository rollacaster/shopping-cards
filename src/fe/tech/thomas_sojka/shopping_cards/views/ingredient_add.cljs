(ns tech.thomas-sojka.shopping-cards.views.ingredient-add
  (:require [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.db :as db]))

(defn main []
  (let [ingredient-name (r/atom "")
        ingredient-category (r/atom :ingredient-category/gewürze)]
    (fn []
      [:div.ph5-ns.flex.flex-column.h-100.pa4
       [:div
        [:div
         [:label.w-100.fw6.mb2.db {:for "ingredient-name"} "Name"]
         [:input.pa1.w-100.mb3
          {:value @ingredient-name
           :name "ingredient-name"
           :on-change #(reset! ingredient-name ^js (.-target.value %))}]]
        [:div
         [:label.w-100.fw6.mb2.db {:for "ingredient-category"} "Kategorie"]
         [:select.pa1.w-100.mb3.mr0
          {:value @ingredient-category
           :name "ingredient-category"
           :on-change #(reset! ingredient-category (keyword (str "ingredient-category/" ^js  (.-target.value %))))}
          (map (fn [category] ^{:key category} [:option {:value category} (name category)])
               db/ingredient-categorys)]]
        [:button.bg-orange-500.white.ph3.pv2.bn.shadow-5.br3
         {:on-click #(dispatch [:transact
                                {:tx-data
                                 [{:ingredient/name @ingredient-name
                                   :ingredient/category {:db/ident @ingredient-category}
                                   :ingredient/id (str (random-uuid))}]
                                 :on-success [:ingredient-add/success]
                                 :on-failure [:ingredient-add/failure]}])}
         "Hinzufügen"]]])))
