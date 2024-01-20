(ns tech.thomas-sojka.shopping-cards.views.recipe-details
  (:require [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c :refer [icon]]
            [reagent.core :as r]))

(defn update-ingredient [recipe ingredient-id value]
  (update recipe :recipe/cooked-with
          (fn [cooked-with]
            (mapv
             (fn [{:keys [ingredient/id] :as c}]
               (if (= ingredient-id id)
                 (merge c value)
                 c))
             cooked-with))))

(defn recipe-ingredient-ids [{:keys [recipe/cooked-with]}]
  (set (map :ingredient/id cooked-with)))

(defn non-recipe-ingredients [ingredients recipe]
  (remove (fn [{:keys [:ingredient/id]}] ((recipe-ingredient-ids recipe) id) )
          ingredients))

(defn remove-ingredient [recipe ingredient-id]
  (update recipe :recipe/cooked-with
          (fn [cooked-with]
            (vec
             (remove (fn [{:keys [ingredient/id]}] (= id ingredient-id))
                     cooked-with)))))

(defn add-ingredient [recipe ingredient]
  (update recipe :recipe/cooked-with conj ingredient))

(defn cooked-with-item [!recipe {:keys [ingredient/id cooked-with/amount cooked-with/unit]} ingredients]
  [:li.flex.items-center.mb1 {:style {:gap 4}}
   [:div.w-20
    [c/input {:value amount :placeholder "200" :class "tr"
              :disabled (nil? id)
              :on-change
              (fn [^js e] (swap! !recipe update-ingredient id {:cooked-with/amount e.target.value}))}]]
   [:div.w-30
    [c/select {:value (or unit "") :placeholder "g"
               :disabled (nil? id)
               :on-change
               (fn [^js e] (swap! !recipe update-ingredient id {:cooked-with/unit e.target.value}))}
     (->> @(subscribe [:ingredients/units])
          (cons "")
          (map
           (fn [t] ^{:key t} [:option.w-100 {:value t} t])))]]
   [:div.w-40
    [c/select {:value (or id "") :on-change
               (fn [e]
                 (let [new-ingredient
                       (some
                        (fn [{:keys [ingredient/id] :as ingredient}] (when (= id e.target.value) ingredient))
                        (non-recipe-ingredients ingredients @!recipe))]
                   (if id
                     (swap! !recipe update-ingredient id new-ingredient)
                     (swap! !recipe add-ingredient new-ingredient))))}
     (->> ingredients
          (sort-by :ingredient/name)
          (map
           (fn [{:keys [ingredient/name ingredient/id]}]
             ^{:key id}
             [:option {:value id} name]))
          (cons ^{:key "none"}
                [:option {:value ""} "Zutat auswählen"])
          doall)]]
   [:div.w-10
    (when id
      [:button.bn.bg-transparent.w-20
       {:on-click #(swap! !recipe remove-ingredient id) :type :button}
       [icon {:class "h1"} :trash-can]])]])

(defn recipe-details [{:keys [!recipe original-recipe]}]
  (let [{:recipe/keys [name image type cooked-with]} @!recipe
        ingredients @(subscribe [:ingredients])]
    [:div.ph5-ns.pt4.pb6.ml2-ns.bg-gray-200
     [:div.ph3
      [:textarea.w-100.mb3.mt0.f2 {:value name
                                   :id "recipe-name"
                                   :on-change (fn [^js e] (swap! !recipe assoc :recipe/name e.target.value))
                                   :style {:resize "none"}}]
      [:button.bn.bg-transparent.flex.bg-orange-700.white.pa2.br2.shadow-5.mb3
       {:on-click #(dispatch [:recipes/delete @!recipe]) :type :button}
       [:span.mr2 "Löschen"]
       [icon {:class "h1"} :trash-can]]
      [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100.mb3
       [:img.w-100.br3.ba.b--orange-300 {:src image}]]]
     [:form {:on-submit
             (fn [^js e]
               (.preventDefault e)
               (dispatch [:recipes/update @!recipe]))}
      [:div.ph3
       [:div.w-100.bg-gray-700.white.pa2.flex.items-center.br2
        [:label.w-40 {:for "recipe-type"}
         "Rezept-Art"]
        [:select.pa1.w-60.br1 {:name "recipe/type"
                               :id "recipe-type"
                               :value type
                               :on-change (fn [^js e] (swap! !recipe assoc :recipe/type e.target.value))}
         (map
          (fn [t]
            ^{:key t}
            [:option.w-100 {:value t} t])
          @(subscribe [:recipes/recipe-types]))]]
       [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
        (doall
         (map
          (fn [item]
            ^{:key (:ingredient/id item)}
            [cooked-with-item !recipe item ingredients])
          cooked-with))
        ^{:key (count cooked-with)}
        [cooked-with-item !recipe {} ingredients]]]
      (when (not= @!recipe original-recipe)
        [:div.fixed.bottom-0.w-100.z-2
         [c/footer {:submit "true" :loading @(subscribe [:app/loading])}]])]]))

(defn main [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [recipe-id]} path
        original-recipe @(subscribe [:recipes/details recipe-id]) ]
    (when original-recipe
      (let [!recipe (r/atom original-recipe)]
        [recipe-details {:original-recipe original-recipe
                         :!recipe !recipe}]))))
