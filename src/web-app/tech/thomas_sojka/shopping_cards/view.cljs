(ns tech.thomas-sojka.shopping-cards.view
  (:require [re-frame.core :refer [dispatch subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]
            [tech.thomas-sojka.shopping-cards.main :as main]))

(defn recipe [{:keys [even name image selected? on-click]}]
  [:button.relative.w-100.w-auto-ns.flex.db-ns.tl.outline-transparent.bg-trbg-gray-600-ns.white-ns.pa0.bt-0.br-0.bl-0.bb-0-ns.bb.b--gray-900.bw1.ml3-ns.mb3-ns.br2-ns.h3.h-auto-ns
   {:on-click on-click :class (if even "bg-gray-600 white" "bg-gray-300")}
   [:div.w5-ns.h5-ns.h-100.shadow-3-ns.w-20.z-1
    (when selected?
      [:<>
       [:div.w3.h3.absolute.orange-400.db.dn-ns
        {:style {:top "50%" :left "2%" :transform "translate(0,-50%)"}}
        [c/icon :check-mark]]
       [:div.w4.h4.absolute.white.dn.db-ns
        {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
        [c/icon :check-mark]]])
    [:img.br2-ns.w-100.h-100 {:style {:object-fit "cover"} :src image :class (when selected? "o-40")}]]
   [:div.bg-gray-700.absolute.pa2.mh2.mb2.bottom-0.o-50.br2.dn.db-ns
    [:span.f4 name]]
   [:div.absolute-ns.pa2.mb2-ns.mh2.bottom-0.w-80.w-auto-ns
    {:class (when selected? "o-40")}
    [:span.f4 name]]])

(defn ingredient [{:keys [i id class]} children]
  [:li.mh2.mh5-ns.ph4.pv3.mt3.br2 {:class [class (if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")]}
   [:label.flex.items-center.pointer.f4 {:for id}
    children]])

(defn ingredient-select [{:keys [i id selected? on-change]} children]
  [ingredient {:i i :id id}
   [:<>
    [:input.pointer.mr3.w2.h2
     {:id id :type "checkbox" :checked selected? :on-change on-change}]
    children]])

(defn finish [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [card-id]} path]
    [:div.flex.justify-center.pv5
     [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
      {:href (str "https://trello.com/c/" card-id)}
      "In Trello anzeigen"]]))

(defn recipe-type-title [recipe-type]
  (case recipe-type
    "NORMAL" [:h2.mv3.tc "Normale Gerichte"]
    "NEW" [:h2.mv3.tc "Neue Gerichte"]
    "FAST" [:h2.mv3.tc "Schnell Gerichte"]
    "MISC" [:h2.mv3.tc "Keine Gerichte"]
    "RARE" [:h2.mv3.tc "Selten"]))


(defn select-recipe [{:keys [recipes get-title]}]
  [:div.flex.db-ns.flex-wrap.justify-center.justify-start-ns.ph5-ns.pb6.pt3-ns
   (map
    (fn [[recipe-type recipe-type-recipes]]
      [:div {:key recipe-type}
       (when (not= (ffirst recipes) recipe-type)
         (get-title recipe-type))
       [:div.flex.flex-wrap
        (map-indexed
         (fn [idx {:keys [id name link image] :as r}]
           [recipe {:key id
                    :even (even? idx)
                    :name name
                    :link link
                    :image image
                    :on-click #(dispatch [:main/add-meal r])}])
         recipe-type-recipes)]])
    (->> recipes
         (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)]))))])

(defn select-lunch []
  (let [recipes @(subscribe [:main/lunch-recipes])]
    [select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 [recipe-type-title recipe-type])}]))

(defn select-dinner []
  (let [recipes @(subscribe [:main/sorted-recipes])]
    [select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 [recipe-type-title recipe-type])}]))

(defn meal-plan-details []
  (fn []
    (let [{{:keys [name link image]} :recipe
           :keys [shopping-list]}
          @(subscribe [:recipe-details/meal])
          ingredients @(subscribe [:recipe-details/ingredients])]
      [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
       [:div.flex.justify-between.items
        [:a.link.near-black.mb3.mb0-ns.db
         {:href link :target "_blank" :referer "norel noopener"
          :class (when-not (empty? link) "underline")}
         [:h1.mv0 name]]
        (when-not shopping-list
          [:button.pv2.br3.bg-orange-200.bn.shadow-2.self-start
           {:on-click #(dispatch [:main/remove-meal])}
           [c/icon {:class "dark-gray h2"} :trash-can]])]
       [:div.flex.justify-between.flex-wrap
        [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100
         [:img.w5.br3.ba.b--orange-300 {:src image}]]
        [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
         (map
          (fn [[id ingredient]]
            [:li.mb3.f4 {:key id} ingredient])
          ingredients)]]
       (when-not (empty? link)
         [:div.flex.justify-center
          [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
           {:href link}
           "Rezept anzeigen"]])])))

(defn header []
  (let [route @(subscribe [:app/route])]
    [:header.bg-orange-400
     [:div.mw9.center
      [:div.pv3.ph5-ns.ph3.flex.justify-between.w-100.items-center
       [:h1.ma0.gray-800.ml2-ns.truncate
        (:title (:data route))]]]]))

(defn app []
  (fn []
    (let [route @(subscribe [:app/route])
          error @(subscribe [:app/error])]
      [:div.sans-serif.flex.flex-column.h-100
       [header]
       (when (:view (:data route))
         [:main.flex-auto
          [:div.mw9.center.bg-gray-200.h-100
           [(:view (:data route)) route]]])
       (when error
         [:div.absolute.white.bottom-0.flex.justify-center.w-100.mb4
          [:div.w-80.bg-light-red.ph3.pv2.br2.ba.b--white
           error]])])))

(defn deselect-ingredients []
  (let [selected-ingredients @(subscribe [:shopping-card/selected-ingredient-ids])
        ingredients @(subscribe [:shopping-card/ingredients])
        loading @(subscribe [:app/loading])
        meals-without-shopping-list @(subscribe [:main/meals-without-shopping-list])]
    [:<>
     [:ul.list.pl0.mv0.pb6
      (map-indexed (fn [i [id content]]
                     [ingredient-select
                      (let [selected? (contains? selected-ingredients id)]
                        {:key id
                         :i i
                         :id id
                         :selected? selected?
                         :on-change
                         #(dispatch [:shopping-card/toggle-selected-ingredients id])})
                      content])
                   ingredients)
      [:button.bn.bg-transparent.w-100.pa0
       {:on-click #(dispatch [:extra-ingredients/show])}
       [ingredient {:i (count ingredients) :id "add-ingredient" :class "ba b--dashed"}
        [:div.flex.items-center
         [:div.w2.flex.items-center.mr3
          [c/icon :add]]
         "Zutat hinzufügen"]]]]
     [:div.fixed.bottom-0.w-100.z-2
      [c/footer {:on-click #(dispatch [:shopping-card/create
                                       meals-without-shopping-list])
                 :app/loading loading}]]]))

(defn add-ingredients []
  (let [ingredients @(subscribe [:extra-ingredients/addable-ingredients])
        ingredient-filter @(subscribe [:extra-ingredients/filter])]
    [:div.ph5-ns.flex.flex-column.h-100
     [:div.ph2.pt3
      [:input.h2.br3.ba.b--gray.ph2 {:value ingredient-filter
                                     :autoFocus true
                                     :on-change (fn [e] (dispatch [:extra-ingredients/filter-ingredients ^js (.-target.value e)]))
                                     :placeholder "Suche..."}]]
     [:ul.list.pl0.mv0.pb6
      (map-indexed (fn [i {:keys [id name]}]
                     [:button.bn.pa0.w-100.ma0.dib
                      {:key id
                       :on-click #(dispatch [:extra-ingredients/add id name])}
                      [ingredient
                       {:i i
                        :id id
                        :selected? false}
                       [:div.flex.w-100
                        [:div.mr3.flex.items-center
                         [c/icon {:style {:width 20}} :add]]
                        name]]])
                   ingredients)]
     (when (seq [])
       [c/footer {:on-click #(dispatch [:add-ingredients []])}])]))

(def routes
  (concat
   main/routes
   [["/meal-plan-details"
     {:name ::meal-plan-details
      :view meal-plan-details
      :title "Rezept"}]
    ["/deselect-ingredients" {:name ::deselect-ingredients
                              :view deselect-ingredients
                              :title "Zutaten auswählen"}]
    ["/add-ingredients" {:name ::add-ingredients
                         :view add-ingredients
                         :title "Zutaten hinzufügen"}]
    ["/finish/:card-id" {:name ::finish
                         :view finish
                         :title "Einkaufszettel erstellt"
                         :parameters {:path {:card-id string?}}}]
    ["/select-lunch"
     {:name ::select-lunch
      :view select-lunch
      :title "Mittag auswählen"}]
    ["/select-dinner"
     {:name ::select-dinner
      :view select-dinner
      :title "Abendessen auswählen"}]]))
