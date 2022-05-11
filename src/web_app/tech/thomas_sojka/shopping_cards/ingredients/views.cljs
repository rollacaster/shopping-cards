(ns tech.thomas-sojka.shopping-cards.ingredients.views
  (:require [re-frame.core :refer [dispatch]]))

(defn ingredients []
  [:div
   [:button.fixed.bottom-0.right-0.bg-orange-500.ma4.pa4.z-1.br-100.relative.shadow-5.bn
    {:type "button"
     :on-click #(dispatch [:ingredients/new])}
    [:span.absolute.f1.white.lh-solid.flex.align-items.justify-center
     {:style {:top "45%"
              :left "50%"
              :transform "translate(-50%,-50%)"}}
     "+"]]])
