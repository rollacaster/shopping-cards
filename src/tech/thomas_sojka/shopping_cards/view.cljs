(ns tech.thomas-sojka.shopping-cards.view
  (:require [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            ["date-fns" :refer (format subDays startOfDay addDays isPast)]
            ["date-fns/locale" :refer (de)]))

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"
            :trash-can "M3 6v18h18v-18h-18zm5 14c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm4-18v2h-20v-2h5.711c.9 0 1.631-1.099 1.631-2h5.315c0 .901.73 2 1.631 2h5.712z"
            :shopping-cart "m 18.595802,0.56054688 c -0.459356,0 -0.816128,0.30784707 -0.967618,0.71344852 L 13.552604,14.165318 H 4.8395597 L 2.3961775,6.9817746 h 9.1724185 c 0.561978,0 1.021353,-0.4593749 1.021353,-1.0213529 0,-0.561978 -0.459375,-1.021353 -1.021353,-1.021353 H 1.0180526 C 0.11400118,4.9830496 -0.11561948,5.8187628 0.05053051,6.2145907 L 3.1584936,15.484745 c 0.1514897,0.405601 0.5620161,0.664581 0.9676175,0.664581 H 14.266147 c 0.459356,0 0.864901,-0.254093 0.967523,-0.664581 L 19.30925,2.5934218 h 3.669941 c 0.561978,0 1.021353,-0.4592795 1.021353,-1.0212574 0,-0.5619779 -0.464262,-1.01161752 -1.021353,-1.01161752 z M 5.0447658,17.98673 c -1.7836692,0 -3.2594339,1.47586 -3.2594339,3.259529 0,1.783669 1.4757647,3.259434 3.2594339,3.259434 1.783669,0 3.259529,-1.475765 3.259529,-3.259434 0,-1.783669 -1.47586,-3.259529 -3.259529,-3.259529 z m 7.9458602,0 c -1.783669,0 -3.2594341,1.47586 -3.2594341,3.259529 0,1.783669 1.4757651,3.259434 3.2594341,3.259434 1.832536,0 3.259529,-1.475765 3.259529,-3.259434 0.0049,-1.783669 -1.47586,-3.259529 -3.259529,-3.259529 z M 5.0447658,20.0197 c 0.7134676,0.0049 1.2754455,0.561959 1.2216911,1.275427 0,0.713467 -0.5619779,1.275426 -1.2216911,1.275426 -0.6646002,0 -1.2216913,-0.561959 -1.2216913,-1.275426 0,-0.713468 0.5619779,-1.275427 1.2216913,-1.275427 z m 7.9508232,0 c 0.708581,0.0049 1.270558,0.561959 1.221691,1.275427 0,0.713467 -0.561978,1.275426 -1.221691,1.275426 -0.659713,0 -1.221691,-0.561959 -1.221691,-1.275426 0,-0.713468 0.561978,-1.275427 1.221691,-1.275427 z"})

(defn icon
  ([name]
   [icon {} name])
  ([{:keys [class]} name]
   [:svg {:viewBox "0 0 24 24" :class class}
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

(defn ingredient [{:keys [i id selected? on-change]} children]
  [:li.mh2.mh5-ns.ph4.pv3.mv3.br2 {:class (if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")}
   [:label.flex.items-center.pointer.f4 {:for id}
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
            (case recipe-type
              "NORMAL" ""
              "FAST" [:h2.mv3.tc "Schnell Gerichte"]
              "RARE" [:h2.mv3.tc "Selten"])
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
    (fn [[recipe-type recipes]]
      [:div {:key recipe-type}
       (get-title recipe-type)
       [:div.flex.flex-wrap
        (map-indexed
         (fn [idx {:keys [id name link image] :as r}]
           [recipe {:key id
                    :even (even? idx)
                    :name name
                    :link link
                    :image image
                    :on-click #(dispatch [:add-meal r])}])
         recipes)]])
    (->> recipes
         (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)]))))])



(defn select-lunch []
  (let [recipes @(subscribe [:lunch-recipes])]
    [select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 (case recipe-type
                                   "NORMAL" [:h2.mv3.tc "Normale Gerichte"]
                                   "FAST" ""
                                   "RARE" [:h2.mv3.tc "Selten"]))}]))

(defn select-dinner []
  (let [recipes @(subscribe [:sorted-recipes])]
    [select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 (case recipe-type
                                   "NORMAL" ""
                                   "FAST" [:h2.mv3.tc "Schnell Gerichte"]
                                   "RARE" [:h2.mv3.tc "Selten"]))}]))

(defn show-recipes []
  (dispatch [:load-recipes])
  (fn []
    (let [sorted-recipes @(subscribe [:sorted-recipes])]
      [:div.flex.flex-wrap.justify-center.justify-start-ns.ph5-ns.pb6.pt3-ns.bg-gray-200
       (doall
        (map
         (fn [[recipe-type recipes]]
           [:div {:key recipe-type}
            (case recipe-type
              "NORMAL" ""
              "FAST" [:h2.ph3 "Schnell Gerichte"]
              "RARE" [:h2.ph3 "Selten"])
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

(defn select-water [ingredients]
  (conj ingredients ["6175d1a2-0af7-43fb-8a53-212af7b72c9c"
                                              "Wasser"]))
(defn deselect-ingredients []
  (let [selected-ingredients @(subscribe [:selected-ingredients])
        ingredients @(subscribe [:ingredients])]
    [:ul.list.pl0.mv0.pb6
     (doall
      (map-indexed (fn [i [id content]]
                     [ingredient
                      (let [selected?
                            (contains? selected-ingredients id)]
                        {:key id
                         :i i
                         :id id
                         :selected? selected?
                         :on-change
                         #(dispatch [:toggle-selected-ingredients id])})
                      content])
                   ingredients))]))

(defn header []
  (let [route @(subscribe [:route])]
    [:header.bg-orange-400
     [:div.mw9.center
      [:div.pv3.ph5-ns.ph3
       [:h1.ma0.gray-800.ml2-ns
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

(defn footer []
  (let [selected-recipes @(subscribe [:selected-recipes])
        route @(subscribe [:route])
        loading @(subscribe [:loading])]
    (when (> (count selected-recipes) 0)
      [:footer.fixed.bottom-0.w-100.bg-orange-400.flex.justify-center.pa3.z-2
       [:button.br3.bg-gray-700.pointer.bn.shadow-3.ph3.pv2.white
        {:on-click (:action (:data route))}
        [:div.flex.items-center
         (if loading
           [:div {:style {:width 128}}
            [spinner]]
           [:<>
            [:span.f2.mr2 "Fertig!"]
            [:span.w2.h2.pt1 [icon {:color "white"} :check-mark]]])]]])))

(defn app []
  (fn []
    (let [route @(subscribe [:route])
          error @(subscribe [:error])]
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
       [footer]])))
(defn add-water [ingredients]
  (conj ingredients "6175d1a2-0af7-43fb-8a53-212af7b72c9c"))

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
     [:h4.f4.fw5.mv0.ba.pa2.br3.h-100.b--gray.bw1
      {:class (r/class-names
               (if has-recipe?
                 (str (if (:shopping-list meal-plan) "bg-gray-400" "bg-orange-400") " white")
                 "b--dashed gray"))}
      (when (:shopping-list meal-plan)
        [:div.w2.absolute.bottom-0.right-0.mr3.mb1
         [icon :shopping-cart]])
      (meal-name meal-plan)]]))

(defn meal-plan []
  (dispatch [:load-recipes])
  (dispatch [:init-meal-plans (js/Date.)])
  (fn []
    (let [meals-plans @(subscribe [:weekly-meal-plans])
          start-of-week @(subscribe [:start-of-week])]
      [:div.ph5-ns.flex.flex-column.h-100
       [:div.flex
        [:div.pv2.w-50
         [:button.pv2.w3.bg-gray-600.ba.br3.br--left.white.b--white
          {:on-click
           #(dispatch [:init-meal-plans (startOfDay (js/Date.))])}
          "Heute"]
         [:button.pv2.w3.bg-gray-600.ba.bl-0.br-0.white.b--white
          {:on-click
           #(dispatch [:init-meal-plans (subDays (startOfDay start-of-week) 6)])}
          "Zurück"]
         [:button.pv2.w3.bg-gray-600.ba.br3.br--right.white.b--white
          {:on-click
           #(dispatch [:init-meal-plans (addDays (startOfDay start-of-week) 6)])}
          "Vor"]]
        [:div.w-50.flex.items-center.justify-center
         (format (:date (ffirst meals-plans)) "MMMM yyyy")]]
       [:div.flex.flex-wrap.flex-auto
        (map
         (fn [[lunch dinner]]
           ^{:key (:date lunch)}
           [:div.ba.w-50.pv2.flex.flex-column.b--gray
            [:div.tr.fw6.ph2 (format (:date lunch) "EEEEEE dd.MM" #js {:locale de})]
            [:div.flex-auto
             {:class (when (isPast (addDays (startOfDay start-of-week) 2)) "o-20")}
             [meal lunch]
             [meal dinner]]])
         meals-plans)]])))

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
   ["/deselect-ingredients" {:name ::deselect-ingredients
                             :view deselect-ingredients
                             :title "Zutaten auswählen"
                             :action #(dispatch [:create-shopping-card])}]
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
