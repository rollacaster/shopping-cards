(ns tech.thomas-sojka.shopping-cards.view
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            ["date-fns" :refer (format subDays startOfDay addDays isPast)]
            ["date-fns/locale" :refer (de)]))

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"
            :trash-can "M3 6v18h18v-18h-18zm5 14c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm4-18v2h-20v-2h5.711c.9 0 1.631-1.099 1.631-2h5.315c0 .901.73 2 1.631 2h5.712z"
            :shopping-cart "m 18.595802,0.56054688 c -0.459356,0 -0.816128,0.30784707 -0.967618,0.71344852 L 13.552604,14.165318 H 4.8395597 L 2.3961775,6.9817746 h 9.1724185 c 0.561978,0 1.021353,-0.4593749 1.021353,-1.0213529 0,-0.561978 -0.459375,-1.021353 -1.021353,-1.021353 H 1.0180526 C 0.11400118,4.9830496 -0.11561948,5.8187628 0.05053051,6.2145907 L 3.1584936,15.484745 c 0.1514897,0.405601 0.5620161,0.664581 0.9676175,0.664581 H 14.266147 c 0.459356,0 0.864901,-0.254093 0.967523,-0.664581 L 19.30925,2.5934218 h 3.669941 c 0.561978,0 1.021353,-0.4592795 1.021353,-1.0212574 0,-0.5619779 -0.464262,-1.01161752 -1.021353,-1.01161752 z M 5.0447658,17.98673 c -1.7836692,0 -3.2594339,1.47586 -3.2594339,3.259529 0,1.783669 1.4757647,3.259434 3.2594339,3.259434 1.783669,0 3.259529,-1.475765 3.259529,-3.259434 0,-1.783669 -1.47586,-3.259529 -3.259529,-3.259529 z m 7.9458602,0 c -1.783669,0 -3.2594341,1.47586 -3.2594341,3.259529 0,1.783669 1.4757651,3.259434 3.2594341,3.259434 1.832536,0 3.259529,-1.475765 3.259529,-3.259434 0.0049,-1.783669 -1.47586,-3.259529 -3.259529,-3.259529 z M 5.0447658,20.0197 c 0.7134676,0.0049 1.2754455,0.561959 1.2216911,1.275427 0,0.713467 -0.5619779,1.275426 -1.2216911,1.275426 -0.6646002,0 -1.2216913,-0.561959 -1.2216913,-1.275426 0,-0.713468 0.5619779,-1.275427 1.2216913,-1.275427 z m 7.9508232,0 c 0.708581,0.0049 1.270558,0.561959 1.221691,1.275427 0,0.713467 -0.561978,1.275426 -1.221691,1.275426 -0.659713,0 -1.221691,-0.561959 -1.221691,-1.275426 0,-0.713468 0.561978,-1.275427 1.221691,-1.275427 z"
            :add "M12 2c5.514 0 10 4.486 10 10s-4.486 10-10 10-10-4.486-10-10 4.486-10 10-10zm0-2c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm6 13h-5v5h-2v-5h-5v-2h5v-5h2v5h5v2z"
            :remove "M12 2c5.514 0 10 4.486 10 10s-4.486 10-10 10-10-4.486-10-10 4.486-10 10-10zm0-2c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm6 13h-12v-2h12v2z"
            :back "M16.67 0l2.83 2.829-9.339 9.175 9.339 9.167-2.83 2.829-12.17-11.996z"})

(defn icon
  ([name]
   [icon {} name])
  ([{:keys [class style]} name]
   [:svg {:viewBox "0 0 24 24" :class class :style style}
    [:path {:d (icons name) :fill "currentColor"}]]))

(defn recipe [{:keys [even name image selected? on-click]}]
  [:button.relative.w-100.w-auto-ns.flex.db-ns.tl.outline-transparent.bg-trbg-gray-600-ns.white-ns.pa0.bt-0.br-0.bl-0.bb-0-ns.bb.b--gray-900.bw1.ml3-ns.mb3-ns.br2-ns.h3.h-auto-ns
   {:on-click on-click :class (if even "bg-gray-600 white" "bg-gray-300")}
   [:div.w5-ns.h5-ns.h-100.shadow-3-ns.w-20.z-1
    (when selected?
      [:<>
       [:div.w3.h3.absolute.orange-400.db.dn-ns
        {:style {:top "50%" :left "2%" :transform "translate(0,-50%)"}}
        [icon :check-mark]]
       [:div.w4.h4.absolute.white.dn.db-ns
       {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
       [icon :check-mark]]])
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


(defn select-recipes []
  (dispatch [:load-recipes])
  (fn []
    (let [selected-recipes @(subscribe [:selected-recipes])
          sorted-recipes @(subscribe [:sorted-recipes])]
      [:div.flex.db-ns.flex-wrap.justify-center.justify-start-ns.ph5-ns.pb6.pt3-ns
       (doall
        (map
         (fn [[recipe-type recipes]]
           [:div {:key recipe-type}
            [recipe-type-title recipe-type]
            [:div.flex.flex-wrap
             (doall
              (map-indexed
               (fn [idx {:keys [id name link image]}]
                 [recipe (let [selected? (contains? selected-recipes id)]
                           {:key id
                            :even (even? idx)
                            :name name
                            :link link
                            :image image
                            :selected? selected?
                            :on-click #(dispatch [:toggle-selected-recipes id])})])
               recipes))]])
         (->> sorted-recipes
              (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)])))))])))

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
                    :on-click #(dispatch [:add-meal r])}])
         recipe-type-recipes)]])
    (->> recipes
         (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)]))))])



(defn select-lunch []
  (let [recipes @(subscribe [:lunch-recipes])]
    [select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 [recipe-type-title recipe-type])}]))

(defn select-dinner []
  (let [recipes @(subscribe [:sorted-recipes])]
    [select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 [recipe-type-title recipe-type])}]))

(defn show-recipes []
  (dispatch [:load-recipes])
  (fn []
    (let [sorted-recipes @(subscribe [:sorted-recipes])]
      [:div.flex.flex-wrap.justify-center.justify-start-ns.ph5-ns.pb6.pt3-ns.bg-gray-200
       (doall
        (map
         (fn [[recipe-type recipes]]
           [:div {:key recipe-type}
            [recipe-type-title recipe-type]
            (doall
             (->> recipes
                  (remove #(or (= (:link %) "") (= (:link %) nil)))
                  (map (fn [{:keys [id name link image]}]
                         [recipe {:key id
                                  :name name
                                  :link link
                                  :image image
                                  :on-click #(dispatch [:show-recipe id])}]))))])
         (->> sorted-recipes
              (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)])))))])))


(defn show-recipe [{{{:keys [recipe-id]} :path}:parameters}]
  (dispatch [:load-ingredients-for-recipe recipe-id])
  (fn [match]
    (let [{:keys [path]} (:parameters match)
          {:keys [recipe-id]} path
          {:keys [name link image]} @(subscribe [:shown-recipe recipe-id])
          ingredients @(subscribe [:recipe-details])]
      [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
       [:a.link.near-black.underline.mb3.mb0-ns.db {:href link :target "_blank" :referer "norel noopener"}
        [:h1.mv0 name]]
       [:div.flex.justify-between.flex-wrap
        [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100
         [:img.w5.br3.ba.b--orange-300 {:src image}]]
        [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
         (map
          (fn [[id ingredient]]
            [:li.mb3.f4 {:key id} ingredient])
          ingredients)]]
       [:iframe.w-100 {:src link :style {:height "50rem"}}]])))

(defn recipe-details [{{{:keys [recipe-id]} :path}:parameters}]
  (dispatch [:load-ingredients-for-recipe recipe-id])
  (fn [match]
    (let [{:keys [path]} (:parameters match)
          {:keys [recipe-id]} path
          {:keys [name link image]} @(subscribe [:shown-recipe recipe-id])
          ingredients @(subscribe [:recipe-details])]
      [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
       [:div.flex.justify-between.items
        [:a.link.near-black.underline.mb3.mb0-ns.db {:href link :target "_blank" :referer "norel noopener"}
         [:h1.mv0 name]]
        [:button.pv2.br3.bg-orange-200.bn.shadow-2.self-start
         {:on-click #(dispatch [:remove-meal])}
         [icon {:class "dark-gray h2"} :trash-can]]]
       [:div.flex.justify-between.flex-wrap
        [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100
         [:img.w5.br3.ba.b--orange-300 {:src image}]]
        [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
         (map
          (fn [[id ingredient]]
            [:li.mb3.f4 {:key id} ingredient])
          ingredients)]]
       [:iframe.w-100 {:src link :style {:height "50rem"}}]])))

(defn meal-plan-details []
  (fn []
    (let [{{:keys [name link image]} :recipe
           :keys [shopping-list]}
          @(subscribe [:selected-meal])
          ingredients @(subscribe [:recipe-details])]
      [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
       [:div.flex.justify-between.items
        [:a.link.near-black.mb3.mb0-ns.db
         {:href link :target "_blank" :referer "norel noopener"
          :class (when-not (empty? link) "underline")}
         [:h1.mv0 name]]
        (when-not shopping-list
          [:button.pv2.br3.bg-orange-200.bn.shadow-2.self-start
           {:on-click #(dispatch [:remove-meal])}
           [icon {:class "dark-gray h2"} :trash-can]])]
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

(defn select-water [ingredients]
  (conj ingredients ["6175d1a2-0af7-43fb-8a53-212af7b72c9c"
                                              "Wasser"]))
(defn header []
  (let [route @(subscribe [:route])]
    [:header.bg-orange-400
     [:div.mw9.center
      [:div.pv3.ph5-ns.ph3.flex.justify-between.w-100.items-center
       [:h1.ma0.gray-800.ml2-ns.truncate
        (:title (:data route))]]]]))

(defn spinner []
  [:svg {:width 38 :height 38
         :viewBox "0 0 100 100"
         :style {:transform "scale(1.8)"}
         :preserveAspectRatio "xMidYMid"}
   [:g
    [:circle {:cx "78.0502" :cy "50" :r "4" :fill "#e15b64"} [:animate {:attributeName "cx" :repeatCount "indefinite" :dur "1s" :values "95;35" :keyTimes "0;1" :begin "-0.67s"}] [:animate {:attributeName "fill-opacity" :repeatCount "indefinite" :dur "1s" :values "0;1;1" :keyTimes "0;0.2;1" :begin "-0.67s"}]]
    [:circle {:cx "38.4502" :cy "50" :r "4" :fill "#e15b64"} [:animate {:attributeName "cx" :repeatCount "indefinite" :dur "1s" :values "95;35" :keyTimes "0;1" :begin "-0.33s"}] [:animate {:attributeName "fill-opacity" :repeatCount "indefinite" :dur "1s" :values "0;1;1" :keyTimes "0;0.2;1" :begin "-0.33s"}]]
    [:circle {:cx "58.2502" :cy "50" :r "4" :fill "#e15b64"} [:animate {:attributeName "cx" :repeatCount "indefinite" :dur "1s" :values "95;35" :keyTimes "0;1" :begin "0s"}] [:animate {:attributeName "fill-opacity" :repeatCount "indefinite" :dur "1s" :values "0;1;1" :keyTimes "0;0.2;1" :begin "0s"}]]]
   [:g {:transform "translate(-15 0)"}
    [:path {:d "M50 50L20 50A30 30 0 0 0 80 50Z" :fill "#f8b26a" :transform "rotate(90 50 50)"}]
    [:path {:d "M50 50L20 50A30 30 0 0 0 80 50Z" :fill "#f8b26a" :transform "rotate(34.8753 50 50)"}
     [:animateTransform {:attributeName "transform" :type "rotate" :repeatCount "indefinite" :dur "1s" :values "0 50 50;45 50 50;0 50 50" :keyTimes "0;0.5;1"}]]
    [:path {:d "M50 50L20 50A30 30 0 0 1 80 50Z" :fill "#f8b26a" :transform "rotate(-34.8753 50 50)"}
     [:animateTransform {:attributeName "transform" :type "rotate" :repeatCount "indefinite" :dur "1s" :values "0 50 50;-45 50 50;0 50 50" :keyTimes "0;0.5;1"}]]]])

(defn footer-action [{:keys [loading]}]
  [:div.flex.items-center
   (if loading
     [:div {:style {:width 128}}
      [spinner]]
     [:<>
      [:span.f2.mr2 "Fertig!"]
      [:span.w2.h2.pt1 [icon {:color "white"} :check-mark]]])])

(defn footer [{:keys [on-click loading]}]
  [:footer.bg-orange-400.flex.justify-center.pa3
   [:button.br3.bg-gray-700.pointer.bn.shadow-3.ph3.pv2.white
    {:on-click on-click}
    [:div.flex.items-center
     (if loading
       [:div {:style {:width 128}}
        [spinner]]
       [:<>
        [:span.f2.mr2 "Fertig!"]
        [:span.w2.h2.pt1 [icon {:color "white"} :check-mark]]])]]])

(defn app []
  (fn []
    (let [route @(subscribe [:route])
          error @(subscribe [:error])
          loading @(subscribe [:loading])
          selected-recipes @(subscribe [:selected-recipes])]
      [:div.sans-serif.flex.flex-column.h-100
       [header]
       (when (:view (:data route))
         [:main.flex-auto
          [:div.mw9.center.bg-gray-200.h-100
           [(:view (:data route)) route]]])
       (when error
         [:div.absolute.white.bottom-0.flex.justify-center.w-100.mb4
          [:div.w-80.bg-light-red.ph3.pv2.br2.ba.b--white
           error]])
       (when (> (count selected-recipes) 0)
         [:div.fixed.bottom-0.w-100.z-2
          [footer {:on-click (:action (:data route))
                   :loading loading}]])])))

(defn deselect-ingredients []
  (let [selected-ingredients @(subscribe [:selected-ingredients])
        ingredients @(subscribe [:recipe-ingredients])
        loading @(subscribe [:loading])
        meals-without-shopping-list @(subscribe [:meals-without-shopping-list])]
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
                         #(dispatch [:toggle-selected-ingredients id])})
                      content])
                   ingredients)
      [:button.bn.bg-transparent.w-100.pa0
       {:on-click #(dispatch [:show-add-ingredients])}
       [ingredient {:i (count ingredients) :id "add-ingredient" :class "ba b--dashed"}
        [:div.flex.items-center
         [:div.w2.flex.items-center.mr3
          [icon :add]]
         "Zutat hinzufügen"]]]]
     [:div.fixed.bottom-0.w-100.z-2
      [footer {:on-click #(dispatch [:create-shopping-card
                                     meals-without-shopping-list])
               :loading loading}]]]))

(defn event->meal-plan [event]
  (cond->
   {:date (.-start ^js event)
    :type (case (.-resource.type ^js event)
            "lunch" :meal-type/lunch
            "dinner" :meal-type/dinner)}
    (.-resource.recipe ^js event)
    (assoc :recipe (js->clj (.-resource.recipe ^js event)
                      :keywordize-keys true))))

(defn meal-name [meal-plan]
  (if (:recipe meal-plan)
    (:name (:recipe meal-plan))
    (case (:type meal-plan)
      :meal-type/lunch "Mittagessen"
      :meal-type/dinner "Abendessen")))

(defn meal [meal-plan]
  (let [has-recipe? (:recipe meal-plan)]
    [:button.pt2.ph2.h-50.bg-transparent.bn.w-100.relative
     {:on-click #(dispatch
                  (if has-recipe?
                    [:show-meal-details meal-plan]
                    [:select-meal meal-plan]))}
     [:div.h-100.br3.bg-center.cover.relative
      {:style {:background-image (if has-recipe? (str "url(" (:image (:recipe meal-plan)) ")") "")}}
      (when has-recipe? [:div.o-40.bg-orange.absolute.h-100.w-100.br3])
      [:h4.f4.fw5.mv0.br3.h-100.bw1.overflow-hidden.flex.justify-center.items-center.absolute.w-100
       {:class (r/class-names (if has-recipe? "white" "ba b--gray b--dashed gray"))}
       (when (:shopping-list meal-plan)
         [:div.absolute.bottom-0.right-0.mr1.bg-orange-400.br-100
          {:style {:width "1.8rem" :padding 5}}
          [icon :shopping-cart]])
       (meal-name meal-plan)]]]))

(defn meal-plan []
  (dispatch [:load-recipes])
  (dispatch [:init-meal-plans (js/Date.)])
  (fn []
    (let [meals-plans @(subscribe [:weekly-meal-plans])
          start-of-week @(subscribe [:start-of-week])
          meals-without-shopping-list @(subscribe [:meals-without-shopping-list])]
      [:div.ph5-ns.flex.flex-column.h-100
       [:div.flex.items-center.justify-between
        [:div.pv2.flex
         [:button.pv2.w3.bg-gray-600.ba.br3.br--left.white.b--white.tc.flex.justify-center
          {:on-click
           #(dispatch [:init-meal-plans (startOfDay (js/Date.))])}
          "Heute"]
         [:button.pv2.w3.bg-gray-600.ba.bl-0.br-0.white.b--white.tc.flex.justify-center
          {:on-click
           #(dispatch [:init-meal-plans (subDays (startOfDay start-of-week) 4)])}
          "Zurück"]
         [:button.pv2.w3.bg-gray-600.ba.br3.br--right.white.b--white.tc.flex.justify-center
          {:on-click
           #(dispatch [:init-meal-plans (addDays (startOfDay start-of-week) 4)])}
          "Vor"]]
        [:div.flex.justify-center.flex-auto
         (format (:date (ffirst meals-plans)) "MMMM yyyy" #js {:locale de})]]
       [:div.flex.flex-wrap.flex-auto
        (doall
         (map
          (fn [[lunch dinner]]
            (let [bank-holiday @(subscribe [:bank-holiday (:date lunch)])]
              ^{:key (:date lunch)}
              [:div.ba.w-50.pv2.flex.flex-column.b--gray.h-50
               [:div.ph2.flex.justify-between
                [:span.truncate.dark-red bank-holiday]
                [:span.tr.fw6
                 {:style {:white-space "nowrap"}}
                 (format (:date lunch) "EEEEEE dd.MM" #js {:locale de})]]
               [:div.flex-auto
                {:class (when (isPast (addDays (startOfDay start-of-week) 2)) "o-20")}
                [meal lunch]
                [meal dinner]]]))
          meals-plans))]
       (when (seq meals-without-shopping-list)
         [footer {:on-click #(dispatch [:load-ingredients-for-meals meals-without-shopping-list])}])])))

(defn add-ingredients []
  (let [ingredients @(subscribe [:addable-ingredients])
        ingredient-filter @(subscribe [:ingredient-filter])]
    [:div.ph5-ns.flex.flex-column.h-100
     [:div.ph2.pt3
      [:input.h2.br3.ba.b--gray.ph2 {:value ingredient-filter
                                     :autoFocus true
                                     :on-change (fn [e] (dispatch [:filter-ingredients ^js (.-target.value e)]))
                                     :placeholder "Suche..."}]]
     [:ul.list.pl0.mv0.pb6
      (map-indexed (fn [i {:keys [id name]}]
                     [:button.bn.pa0.w-100.ma0.dib
                      {:key id
                       :on-click #(dispatch [:add-extra-ingredient id name])}
                      [ingredient
                       {:i i
                        :id id
                        :selected? false}
                       [:div.flex.w-100
                        [:div.mr3.flex.items-center
                         [icon {:style {:width 20}} :add]]
                        name]]])
                   ingredients)]
     (when (seq [])
       [footer {:on-click #(dispatch [:add-ingredients []])}])]))

(def routes
  [["/" {:name ::main
         :view select-recipes
         :title "Rezepte"
         :action #(dispatch [:load-ingredients-for-selected-recipes])}]
   ["/show-recipes"
    {:name ::recipes
     :view show-recipes
     :title "Rezepte"}]
   ["/show-recipes/:recipe-id"
    {:name ::recipe
     :view show-recipe
     :title "Rezept"
     :parameters {:path {:recipe-id string?}}}]
   ["/recipes/:recipe-id"
    {:name ::recipe-details
     :view recipe-details
     :title "Rezept"
     :parameters {:path {:recipe-id string?}}}]
   ["/meal-plan-details"
    {:name ::meal-plan-details
     :view meal-plan-details
     :title "Rezept"}]
   ["/deselect-ingredients" {:name ::deselect-ingredients
                             :view deselect-ingredients
                             :title "Zutaten auswählen"
                             :action #(dispatch [:create-shopping-card])}]
   ["/add-ingredients" {:name ::add-ingredients
                        :view add-ingredients
                        :title "Zutaten hinzufügen"}]
   ["/finish/:card-id" {:name ::finish
                        :view finish
                        :title "Einkaufszettel erstellt"
                        :parameters {:path {:card-id string?}}
                        :action #(dispatch [:restart])}]
   ["/meal-plan" {:name ::meal-plan
                  :view meal-plan
                  :title "Essensplan"}]
   ["/select-lunch"
    {:name ::select-lunch
     :view select-lunch
     :title "Mittag auswählen"}]
   ["/select-dinner"
    {:name ::select-dinner
     :view select-dinner
     :title "Abendessen auswählen"}]])
