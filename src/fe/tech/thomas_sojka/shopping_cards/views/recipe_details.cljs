(ns tech.thomas-sojka.shopping-cards.views.recipe-details
  (:require [clojure.set :as set]
            [fork.reagent :as fork]
            [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c :refer [icon]]))

(defn cooked-with-c [a {:fieldarray/keys [fields remove insert handle-blur set-handle-change]}]
  [:<>
   (doall
    (map-indexed
     (fn [idx [{:cooked-with/keys [amount-desc amount unit]} ingredient]]
       (let [ingredient-id (str (random-uuid))]
         ^{:key (or (:ingredient/id ingredient) ingredient-id)}
         [:div.bg-gray-700.white.pa2.mb3.br2
          [:div.flex.justify-between.items-center.mb3
           (if (:ingredient/name ingredient)
             [:div.f3.fw3.mr2.w-90 (:ingredient/name ingredient)]
             (let [ingredients @(subscribe [:ingredients])]
               [:select.pa1.w-100.mr4
                {:on-change (fn [^js e]
                              (set-handle-change
                               {:value (first
                                        (get
                                         (set/index
                                          (set @(subscribe [:ingredients]))
                                          [:ingredient/id])
                                         {:ingredient/id e.target.value}))
                                :path [:ingredients idx 1]}))}
                (->> ingredients
                     (sort-by :ingredient/name)
                     (map
                      (fn [{:keys [ingredient/name ingredient/id]}]
                        ^{:key id}
                        [:option {:value id} name]))
                     (cons ^{:key "none"}
                           [:option {:value ""} "Zutat auswählen"]))]))
           [:button.bn.bg-orange-200.shadow-1.pa2.br3.w-10
            {:on-click #(remove idx)
             :type :button}
            [icon {:class "h1"} :trash-can]]]
          [:div.flex.flex-column
           [:div.flex.mb1.items-center
            [:label.w-40 {:for "amount-desc"} "Beschreibung"]
            [:input.pa1.w-60.br1.border-box.bn
             {:value amount-desc
              :autoComplete "off"
              :name "cooked-with/amount-desc"
              :on-change (fn [^js e]
                           (set-handle-change
                            {:value (.-target.value e)
                             :path [:ingredients idx 0 :cooked-with/amount-desc]}))
              :on-blur #(handle-blur % idx)}]]
           [:div.flex.mb1.items-center
            [:label.w-40 {:for "amount"} "Menge"]
            [:input.pa1.w-60.br1.border-box.bn
             {:value amount
              :autoComplete "off"
              :type "number"
              :name "cooked-with/amount"
              :on-change (fn [^js e]
                           (set-handle-change
                            {:value (.-target.value e)
                             :path [:ingredients idx 0 :cooked-with/amount]}))
              :on-blur #(handle-blur % idx)}]]
           [:div.flex.mb1.items-center
            [:label.w-40 {:for "unit"} "Einheit"]
            [:input.pa1.w-60.br1.border-box.bn
             {:value unit
              :autoComplete "off"
              :name "cooked-with/unit"
              :on-change (fn [^js e]
                           (set-handle-change
                            {:value (.-target.value e)
                             :path [:ingredients idx 0 :cooked-with/unit]}))
              :on-blur #(handle-blur % idx)}]]]]))
     fields))

   (when (every? (comp :ingredient/id second) fields)
     [:div.flex.justify-center
      [:button.button.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
       {:type :button
        :on-click #(insert [{:cooked-with/id (str (random-uuid))
                             :cooked-with/amount-desc "1"
                             :cooked-with/amount 1}
                            nil])}
       "Add ingredient"]])])

(defn recipe-details [{:keys [recipe]}]
  (let [{:keys [name image]} recipe]
    [:div.ph5-ns.pt4.pb6.ml2-ns.bg-gray-200
     [:div.ph3
      [:h1.mb3.mt0 name]
      [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100.mb3
       [:img.w-100.br3.ba.b--orange-300 {:src image}]]]
     [fork/form {:key recipe
                 :initial-values recipe
                 :prevent-default? true
                 :keywordize-keys true
                 :on-submit (fn [{:keys [_ _ values]}]
                              (dispatch [:recipes/update values]))}
      (fn [{:keys [form-id handle-submit values set-handle-change handle-blur dirty] :as props}]
        [:form {:id form-id :on-submit handle-submit}
         [:div.ph3
          [:div.w-100.bg-gray-700.white.pa2.flex.items-center.br2
           [:label.w-40 {:for "recipe-type"}
            "Rezept-Art"]
           [:select.pa1.w-60.br1 {:name "recipe/type"
                                  :id "recipe-type"
                                  :value (:recipe/type values)
                                  :on-change (fn [^js e]
                                               (set-handle-change
                                                {:value (keyword (str "recipe-type/" (.-target.value e)))
                                                 :path [:recipe/type]}))
                                  :on-blur handle-blur}
            (map
             (fn [t]
               ^{:key t}
               [:option.w-100 {:value t} t])
             @(subscribe [:recipes/recipe-types]))]]
          [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
           [fork/field-array {:props props
                              :name :ingredients}
            cooked-with-c]]]
         (when (and dirty
                    (every? (comp :ingredient/id second)(:ingredients values)))
           [:div.fixed.bottom-0.w-100.z-2
            [c/footer {:submit "true" :loading @(subscribe [:app/loading])}]])])]]))

(defn main [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [recipe-id]} path
        recipe @(subscribe [:recipes/details recipe-id])]

    [recipe-details {:recipe (update recipe :ingredients vec)}]))
