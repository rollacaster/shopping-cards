(ns tech.thomas-sojka.shopping-cards.edit-recipes.views
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [tech.thomas-sojka.shopping-cards.components
    :refer
    [icon recipe]]))

(defn recipes-editing []
  (let [recipes @(subscribe [:main/recipes])]
    [:<>
     (->> recipes
          (sort-by :name)
          (map
           (fn [{:keys [id name image]}]
             ^{:key id}
             [recipe {:name name :image image :on-click #(dispatch [:edit-recipe/show-recipe id])}])))]))

(defn recipe-details [{:keys [id name image ingredients add-new-ingredient type]}]
  [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
   [:h1.mb3.mt0 name]
   [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100.mb3
    [:img.w-100.br3.ba.b--orange-300 {:src image}]]
   [:div.w-100.bg-gray-700.white.pa2.flex.items-center.br2
    [:label.w-40 {:for "recipe-type"}
     "Rezept-Art"]
    [:select.pa1.w-60.br1 {:name "type" :id "recipe-type"
                           :value type
                           :on-change (fn [e] (dispatch [:edit-recipe/edit-type id  ^js (.-target.value e)]))}
     (map
      (fn [t]
        ^{:key t}
        [:option.w-100 {:value t} t])
      @(subscribe [:edit-recipe/recipe-types]))]]
   [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
    ingredients]
   [:div.flex.justify-center
    [:button.button.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
     {:on-click add-new-ingredient}
     "Add ingredient"]]])

(defn recipe-editing [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [recipe-id]} path
        {:keys [id name image type cooked-with]} @(subscribe [:edit-recipe/recipe-details recipe-id])]
    [recipe-details
     {:id id
      :name name
      :image image
      :type type
      :add-new-ingredient #(dispatch [:edit-recipe/show-add-ingredient id])
      :ingredients (map
                    (fn [{:keys [amount-desc amount unit ingredient]}]
                      ^{:key id}
                      [:div.bg-gray-700.white.pa2.mb3.br2
                       [:div.flex.justify-between.items-center.mb3
                        [:div.f3.fw3.mr2 (:name ingredient)]
                        [:button.bn.bg-orange-200.shadow-1.pa2.br3
                         {:on-click #(dispatch [:edit-recipe/remove-ingredient recipe-id id])}
                         [icon {:class "h1"} :trash-can]]]
                       [:div.flex.flex-column
                        [:div.flex.mb1.items-center
                         [:label.w-40 {:for "amount-desc"} "Beschreibung"]
                         [:input.pa1.w-60.br1 {:value amount-desc :id "amount-desc"}]]
                        [:div.flex.mb1.items-center
                         [:label.w-40 {:for "amount"} "Menge"]
                         [:input.pa1.w-60.br1 {:value amount :id "amount"}]]
                        [:div.flex.mb1.items-center
                         [:label.w-40 {:for "unit"} "Einheit"]
                         [:input.pa1.w-60.br1 {:value unit :id "unit"}]]]])
                    cooked-with)
      :on-remove #(prn "deleted")}]))

(defn ingredient [{:keys [i id class]} children]
  [:li.mh2.mh5-ns.ph4.pv3.mt3.br2 {:class [class (if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")]}
   [:label.flex.items-center.pointer.f4 {:for id}
    children]])

(defn add-ingredient [match]
  (let [ingredients @(subscribe [:edit-recipe/all-ingredients])
        {:keys [path]} (:parameters match)
        {:keys [recipe-id]} path]
    [:ul.list.pl0.mv0.pb6
     (map-indexed (fn [i {:keys [id name]}]
                    [:button.bn.pa0.w-100.ma0.dib
                     {:key id
                      :on-click #(dispatch [:edit-recipe/add-ingredient recipe-id id])}
                     [ingredient
                      {:i i
                       :id id
                       :selected? false}
                      [:div.flex.w-100
                       [:div.mr3.flex.items-center
                        [icon {:style {:width 20}} :add]]
                       name]]])
                  ingredients)]))
