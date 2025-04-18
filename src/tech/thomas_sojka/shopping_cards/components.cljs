(ns tech.thomas-sojka.shopping-cards.components
  (:require ["date-fns" :refer (startOfDay)]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.icons :as icons]))

(defn icon
  ([name]
   [icon {} name])
  ([{:keys [class style]} name]
   [:svg {:viewBox "0 0 24 24" :class class :style style}
    [:path {:d (icons/svg-paths name) :fill "currentColor"}]]))

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

(defn footer [{:keys [on-click loading submit]}]
  [:footer.bg-orange-400.flex.justify-center.pa3
   [:button.br3.bg-gray-700.pointer.bn.shadow-3.ph3.pv2.white
    (cond-> {:disabled loading}
      on-click (assoc :on-click on-click)
      submit (assoc :submit submit))
    [:div.flex.items-center
     (if loading
       [:div {:style {:width 128}}
        [spinner]]
       [:<>
        [:span.f2.mr2 "Fertig!"]
        [:span.w2.h2.pt1 [icon {:color "white"} :check-mark]]])]]])

(defn ingredient [{:keys [i id class]} children]
  [:li.mh5-ns.ph4.pv3 {:class [class (if (= (mod i 2) 0) "bg-gray-600 white" "bg-orange-300 gray-700")]}
   [:label.flex.items-center.pointer.f4 {:for id}
    children]])

(defn recipe-type-title [recipe-type]
  (case recipe-type
    :recipe-type/normal [:h2.mv3.tc "Normale Gerichte"]
    :recipe-type/fast [:h2.mv3.tc "Schnell Gerichte"]
    :recipe-type/misc [:h2.mv3.tc "Keine Gerichte"]
    :recipe-type/rare [:h2.mv3.tc "Selten"]
    :recipe-type/party-food [:h2.mv3.tc "Party Food"]))

(defn recipe-rating [rating]
  (when rating
    [:div.absolute.right-1.top-1
     (case rating
       "red" [:div.w2.h2.bg-red.ba.b--black {:style {:border-radius "100%"}}]
       "yellow" [:div.w2.h2.bg-yellow.ba.b--black {:style {:border-radius "100%"}}]
       "green" [:div.w2.h2.bg-green.ba.b--black {:style {:border-radius "100%"}}]
       nil)]))

(defn recipe [{:keys [even name image selected? on-click rating]}]
  [:button.relative.w-100.w-auto-ns.flex.db-ns.tl.outline-transparent.bg-gray-600-ns.white-ns.pa0.bt-0.br-0.bl-0.bb-0-ns.bb.b--gray-900.bw1.ml3-ns.mb3-ns.br2-ns.h3.h-auto-ns
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
    {:aria-hidden "true"}
    [:span.f4 name]]
   [recipe-rating rating]
   [:div.absolute-ns.pa2.mb2-ns.mh2.bottom-0.w-80.w-auto-ns
    {:class (when selected? "o-40")}
    [:span.f4 name]]])

(defn search-filter [{:keys [value on-change]}]
  [:div.w-100
   [:input.pv3.bn.pl4.w-100.border-box.bg-orange-100.f4
    {:placeholder "Suche ..."
     :value value
     :on-change on-change}]])

(def input-class "border-box ph2 pv3 w-100 bn br2")
(def disbled-class"bg-gray-400 gray-500")

(defn input-box [label input]
  [:div.flex.flex-column.mb3 {:style {:gap "1rem"}}
   label
   input])

(defn label [{:keys [for]} children]
  [:label.w-100.fw6.db {:for for} children])

(defn input [{:keys [type autoComplete required value name on-change class style placeholder disabled]}]
  [:input.border-box.ph2.pv3.w-100.bn.br2
   (cond->
       {:name name
        :class (cond-> (str input-class " " class)
                 disabled (str " " disbled-class))
        :type type
        :autoComplete autoComplete
        :required required
        :style style
        :placeholder placeholder
        :disabled disabled}
       value (assoc :value value)
       on-change (assoc :on-change on-change))])

(defn select [{:keys [value name on-change disabled class]} children]
  [:select
   {:value value
    :name name
    :on-change on-change
    :class (cond-> (str input-class " " class)
             disabled (str " " disbled-class))
    :disabled disabled}
   children])

(defn select-recipe []
  (let [filter-value (r/atom "")]
    (fn [{:keys [recipes get-title type date]}]
      [:<>
       [search-filter {:value @filter-value
                       :on-change (fn [event] (reset! filter-value ^js (.-target.value event)))}]
       [:div.flex.db-ns.flex-wrap.justify-center.justify-start-ns.ph5-ns.pb6.pt3-ns
        (doall
         (->> recipes
              (map (fn [[recipe-type recipe-type-recipes]]
                     [recipe-type (sort-by :recipe/name recipe-type-recipes)]))
              (map
               (fn [[recipe-type recipe-type-recipes]]
                 [:div.w-100 {:key recipe-type}
                  (when (not= (ffirst recipes) recipe-type)
                    (get-title recipe-type))
                  [:div.flex.flex-wrap
                   (doall
                    (->> recipe-type-recipes
                         (filter (fn [{:keys [recipe/name]}]
                                   (str/includes? (str/lower-case name) (str/lower-case @filter-value))))
                         (map-indexed
                          (fn [idx {:recipe/keys [id link image rating] :as r}]
                            [recipe {:key id
                                     :even (even? idx)
                                     :name (:recipe/name r)
                                     :link link
                                     :image image
                                     :rating rating
                                     :on-click (fn []
                                                 (dispatch [:meal/add {:recipe r
                                                                       :type type
                                                                       :date (startOfDay (js/Date. date))}]))}]))))]]))))]])))
(defn button
  ([children]
   [button {} children])
  ([{:keys [on-click type]} children]
   [:button.bg-orange-400.bn.ph4.pv3.br2.shadow-5.fw6
    {:on-click on-click
     :type type}
    children]))

(defn add-button [{:keys [href]}]
  [:a.fixed.bottom-2.right-0.bg-orange-500.ma4.pa4.z-1.br-100.relative.shadow-5.bn
   {:href href}
   [:span.absolute.f1.white.lh-solid.flex.align-items.justify-center
    {:style {:top "45%"
             :left "50%"
             :transform "translate(-50%,-50%)"}}
    "+"]])
