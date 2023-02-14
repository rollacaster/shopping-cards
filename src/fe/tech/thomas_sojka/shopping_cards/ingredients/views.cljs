(ns tech.thomas-sojka.shopping-cards.ingredients.views
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [tech.thomas-sojka.shopping-cards.main.components :as c]))

(defn ingredients []
  (dispatch [:ingredients/load])
  (fn []
    (let [ingredients @(subscribe [:ingredients/all])]
      [:div.bg-gray-200
       [:ul.pl0.mv0
        (->> ingredients
             (sort-by :ingredient/name)
             (map-indexed
              (fn [i {:ingredient/keys [id name]}]
                ^{:key id}
                [c/ingredient
                 {:i i
                  :id id
                  :selected? false}
                 [:div.flex.w-100
                  name]])))]
       [:button.fixed.bottom-0.right-0.bg-orange-500.ma4.pa4.z-1.br-100.relative.shadow-5.bn
        {:type "button"
         :on-click #(dispatch [:ingredients/new])}
        [:span.absolute.f1.white.lh-solid.flex.align-items.justify-center
         {:style {:top "45%"
                  :left "50%"
                  :transform "translate(-50%,-50%)"}}
         "+"]]])))
