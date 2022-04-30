(ns tech.thomas-sojka.shopping-cards.edit-recipes.views
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [fork.reagent :as fork]
   [re-frame.core :refer [dispatch subscribe]]
   [tech.thomas-sojka.shopping-cards.components :refer [icon recipe]]
   [tech.thomas-sojka.shopping-cards.main.components :as c]))

(defn recipes-editing []
  (let [recipes @(subscribe [:main/recipes])]
    [:<>
     (->> recipes
          (sort-by :name)
          (map
           (fn [{:keys [id name image]}]
             ^{:key id}
             [recipe {:name name :image image :on-click #(dispatch [:edit-recipe/show-recipe id])}])))]))

(defn cooked-with-c [_ {:fieldarray/keys [fields remove insert handle-change handle-blur set-handle-change]}]
  [:<>
   (map-indexed
    (fn [idx {:cooked-with/keys [id ingredient amount-desc amount unit]}]
      ^{:key id}
      [:div.bg-gray-700.white.pa2.mb3.br2
       [:div.flex.justify-between.items-center.mb3
        [:input.f3.fw3.mr2.w-90 {:value (:ingredient/name ingredient)
                            :name "cooked-with/ingredient"
                            :on-change (fn [^js e]
                                         (set-handle-change
                                          {:value (.-target.value e)
                                           :path [:cooked-with/_recipe idx :cooked-with/ingredient :ingredient/name]}))
                            :on-blur #(handle-blur % idx) }]
        [:button.bn.bg-orange-200.shadow-1.pa2.br3.w-10
         {:on-click #(remove idx)
          :type :button}
         [icon {:class "h1"} :trash-can]]]
       [:div.flex.flex-column
        [:div.flex.mb1.items-center
         [:label.w-40 {:for "amount-desc"} "Beschreibung"]
         [:input.pa1.w-60.br1 {:value amount-desc
                               :name "cooked-with/amount-desc"
                               :on-change #(handle-change % idx)
                               :on-blur #(handle-blur % idx)}]]
        [:div.flex.mb1.items-center
         [:label.w-40 {:for "amount"} "Menge"]
         [:input.pa1.w-60.br1 {:value amount
                               :name "cooked-with/amount"
                               :on-change #(handle-change % idx)
                               :on-blur #(handle-blur % idx)}]]
        [:div.flex.mb1.items-center
         [:label.w-40 {:for "unit"} "Einheit"]
         [:input.pa1.w-60.br1 {:value unit
                               :name "cooked-with/unit"
                               :on-change #(handle-change % idx)
                               :on-blur #(handle-blur % idx)}]]]])
    fields)
   [:div.flex.justify-center
    [:button.button.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
     {:type :button
      :on-click #(insert {:cooked-with/id (str (random-uuid))
                          :cooked-with/amount-desc "1"
                          :cooked-with/amount 1})}
     "Add ingredient"]]])
(defn recipe-details [{:keys [recipe]}]
  (let [{:recipe/keys [image]
         recipe-name :recipe/name} recipe]
    [:div.ph5-ns.pv4.ml2-ns.bg-gray-200
     [:div.ph3
      [:h1.mb3.mt0 recipe-name]
      [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100.mb3
       [:img.w-100.br3.ba.b--orange-300 {:src image}]]]
     (when recipe
       [fork/form {:key recipe
                   :initial-values recipe
                   :prevent-default? true
                   :keywordize-keys true
                   :on-submit (fn [{:keys [_ _ values]}]
                                (let [removed-cooked-with (mapv
                                                           (fn [id]
                                                             [:db/retractEntity [:cooked-with/id id]])
                                                           (set/difference
                                                            (set (map :cooked-with/id (:cooked-with/_recipe recipe)))
                                                            (set (map :cooked-with/id (:cooked-with/_recipe values)))))
                                      updated-recipe (->> values
                                                          :cooked-with/_recipe
                                                          (mapv
                                                           (fn [c] (cond-> c
                                                                    true (assoc :cooked-with/recipe (dissoc values :cooked-with/_recipe))
                                                                    (:cooked-with/amount c) (update :cooked-with/amount float)))))]
                                  (dispatch [:transact (into updated-recipe removed-cooked-with)])))}
        (fn [{:keys [form-id handle-submit values set-handle-change handle-blur dirty] :as props}]
          [:form {:id form-id
                  :on-submit handle-submit}
           [:div.ph3
            [:div.w-100.bg-gray-700.white.pa2.flex.items-center.br2
             [:label.w-40 {:for "recipe-type"}
              "Rezept-Art"]
             [:select.pa1.w-60.br1 {:name "recipe/type"
                                    :id "recipe-type"
                                    :value (str "recipe-type/" (name (:db/ident (:recipe/type values))))
                                    :on-change (fn [^js e]
                                                 (set-handle-change
                                                  {:value (keyword (.-target.value e))
                                                   :path [:recipe/type :db/ident]}))
                                    :on-blur handle-blur}
              (map
               (fn [t]
                 ^{:key t}
                 [:option.w-100 {:value (str "recipe-type/" (str/lower-case t))} t])
               @(subscribe [:edit-recipe/recipe-types]))]]]
           [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns.ph3
            [fork/field-array {:props props
                               :name :cooked-with/_recipe}
             cooked-with-c]]
           (when (or dirty (not= (count (:cooked-with/_recipe recipe))
                                 (count (:cooked-with/_recipe values))))
             [:div.fixed.bottom-0.w-100.z-2
              [c/footer {:submit "true" :loading @(subscribe [:app/loading])}]])])])]))

(defn recipe-editing [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [recipe-id]} path
        recipe @(subscribe [:edit-recipe/recipe-details recipe-id])]
    [recipe-details
     {:recipe recipe}]))

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
