(ns tech.thomas-sojka.shopping-cards.recipe-add.views
  (:require
   [re-frame.core :refer [dispatch]]
   [reagent.core :as r]))

(defn new-recipe []
  (r/with-let [recipe-url (r/atom "")
               recipe-image-url (r/atom "")]
    [:div.ph5-ns.flex.flex-column.h-100.pa4
     [:div
      [:div
       [:label.w-100.fw6.mb2.db {:for "recipe-url"} "Rezept-URL"]
       [:input.pa1.w-100.mb3
        {:value @recipe-url
         :name "recipe-url"
         :on-change #(reset! recipe-url ^js (.-target.value %))}]]
      [:div
       [:label.w-100.fw6.mb2.db {:for "recipe-image-url"} "Rezept-Bild-URL"]
       [:input.pa1.w-100.mb3
        {:value @recipe-image-url
         :name "recipe-image-url"
         :on-change #(reset! recipe-image-url ^js (.-target.value %))}]]
      [:button.bg-orange-500.white.ph3.pv2.bn.shadow-5.br3
       {:on-click #(dispatch [:recipe-add/new {:link @recipe-url
                                               :image @recipe-image-url}])}
       "Hinzufuegen"]]]))
