(ns tech.thomas-sojka.shopping-cards.recipes.views
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [tech.thomas-sojka.shopping-cards.components :refer [recipe]]))

(defn recipes-editing []
  (let [recipes @(subscribe [:main/recipes])]
    [:div.pb6.bg-gray-300
     (->> recipes
          (sort-by :name)
          (map
           (fn [{:keys [id name image]}]
             ^{:key id}
             [recipe {:name name :image image :on-click #(dispatch [:recipes/show-recipe id])}])))
     [:button.fixed.bottom-0.right-0.bg-orange-500.ma4.pa4.z-1.br-100.relative.shadow-5.bn
      {:type "button"
       :on-click #(dispatch [:recipes/new])}
      [:span.absolute.f1.white.lh-solid.flex.align-items.justify-center
       {:style {:top "45%"
                :left "50%"
                :transform "translate(-50%,-50%)"}}
       "+"]]]))
