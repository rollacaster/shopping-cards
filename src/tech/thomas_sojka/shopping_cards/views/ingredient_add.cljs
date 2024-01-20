(ns tech.thomas-sojka.shopping-cards.views.ingredient-add
  (:require [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.components :as c]
            [tech.thomas-sojka.shopping-cards.specs :as specs]))

(defn main [match]
  (let [{:keys [path]} (:parameters match)
        ingredient-name (r/atom (:ingredient-name path))
        ingredient-category (r/atom :ingredient-category/gewürze)]
    (fn []
      [:div.ph5-ns.flex.flex-column.h-100.pa4
       [:div
        [c/input-box
         [c/label {:for "ingredient-name"} "Name"]
         [c/input {:value @ingredient-name
                   :name "ingredient-name"
                   :on-change #(reset! ingredient-name ^js (.-target.value %))}]]
        [c/input-box
         [c/label {:for "ingredient-category"} "Kategorie"]
         [c/select
          {:value @ingredient-category
           :name "ingredient-category"
           :on-change #(reset! ingredient-category (keyword (str "ingredient-category/" ^js  (.-target.value %))))}
          (map (fn [category] ^{:key category} [:option {:value category} (name category)])
               specs/ingredient-categorys)]]
        [c/button
         {:type "button"
          :on-click #(dispatch [:ingredients/add
                                {:ingredient/name @ingredient-name
                                 :ingredient/category @ingredient-category}])}
         "Hinzufügen"]]])))
