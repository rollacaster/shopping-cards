(ns tech.thomas-sojka.ingredients.core
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as s]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"})

(defn icon [{:keys [color]} name]
  [:svg {:viewBox "0 0 24 24"}
   [:path {:d (icons name) :fill color}]])

(defn recipe [{:keys [name image selected? on-click]}]
  [:button.bn.bg-transparent.outline-transparent {:on-click on-click}
   [:div.w5.h5.relative.ma2.shadow-3 {:class (when selected? "o-50")}
    (when selected?
      [:div.w4.h4.absolute {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
       [icon {:color "white"} :check-mark]])
    [:img.br2.w-100.h-100 {:style {:object-fit "cover"} :src image}]
    [:div.bg-dark-gray.absolute.pa2.mh2.mb2.bottom-0.o-50.br2
     [:span.white.f4 name]]
    [:div.absolute.pa2.mh2.mb2.bottom-0
     [:span.white.f4 name]]]])

(defn ingredient [{:keys [i id selected? on-change]} children]
  [:li.flex.items-center.ph4.pv3 {:class (if (= (mod i 2) 0) "bg-light-gray near-black" "bg-orange white")}
   [:input.pointer.mh2
    {:id id :type "checkbox" :checked selected? :on-change on-change}]
   [:label.pointer.f4 {:for id} children]])

(def recipes (r/atom []))
(def selected-recipes (r/atom #{"dd3fa340-a54a-4dc8-aea2-68cdc3656608"}))
(def selected-ingredients (r/atom #{}))
(def ingredients (r/atom []))
(def loading (r/atom false))

(defn open-tab [url]
  (let [a (.createElement js/document "a")
        e (.createEvent js/document "MouseEvents")]
    (set! (.-target a) "_blank")
    (set! (.-href a) url)
    (.initMouseEvent e "click", true, true, js/window, 0, 0, 0, 0, 0, false, false, false, false, 0, nil)
    (.dispatchEvent a e)))

(defn app []
  (let [step (r/atom "SELECT_RECIPE")]
    (-> (.fetch js/window "http://192.168.178.50:3000/recipes")
        (.then #(.json %))
        (.then #(js->clj % :keywordize-keys true))
        (.then #(reset! recipes %)))
    (fn []
      [:div.sans-serif.near-black
       [:header.bg-gold
        [:div.mw9.center
         [:div.pv3.ph4
          [:div.ml2
           [:h1.ma0
            (case @step
              "SELECT_RECIPE" "Select Recipes"
              "DESELECT_INGREDIENTS" "Remove available ingredients"
              "TODO")]]]]]
       [:main.bg-light-gray        
        [:div.mw9.center
         (case @step
           "SELECT_RECIPE"
           [:div.flex.flex-wrap.justify-center.justify-start-ns.ph4.pb6.pt3
            (doall
             (map (fn [{:keys [id name link image]}]
                    [recipe (let [selected? (contains? @selected-recipes id)]
                              {:key id
                               :name name
                               :link link
                               :image image
                               :selected? selected?
                               :on-click #(swap! selected-recipes
                                                 (fn [selected-recipes]
                                                   ((if selected? disj conj)
                                                    selected-recipes id)))})])
                  @recipes))]
           "DESELECT_INGREDIENTS"
           [:ul.list.pl0.mv0.pb6
            (doall
             (map-indexed (fn [i [id content]]
                            [ingredient
                             (let [selected?
                                   (contains? @selected-ingredients id)]
                               {:key id
                                :i i
                                :id id
                                :selected? selected?
                                :on-change
                                #(swap! selected-ingredients
                                        (fn [selected-ingredients]
                                          ((if selected? disj conj)
                                           selected-ingredients id)))})
                             content])
                          @ingredients))]
           "TODO")]]
       (when (and (> (count @selected-recipes) 0))
         [:footer.fixed.bottom-0.w-100.bg-gold.flex.justify-center.pa3
          [:button.br3.bg-gray.pointer.bn.shadow-3.ph3.pv2.white
           {:on-click (fn []
                        (case @step
                          "SELECT_RECIPE"
                          (do
                            (reset! loading true)
                            (-> (.fetch js/window (str "http://192.168.178.50:3000/ingredients?"
                                                        (s/join "&" (map #(str "recipe-ids=" %) @selected-recipes))))
                                 (.then #(.text %))
                                 (.then read-string)
                                 (.then #(do
                                           (reset! loading false)
                                           (reset! step "DESELECT_INGREDIENTS")
                                           (reset! ingredients %)
                                           (reset! selected-ingredients (set (map first %)))
                                           (.scrollTo js/window 0 0)))))
                          "DESELECT_INGREDIENTS"
                          (do
                            (reset! loading true)
                            (-> (.fetch js/window "http://192.168.178.50:3000/shopping-card"
                                         (clj->js {:method "POST"
                                                   :headers {"Content-type" "application/edn"}
                                                   :body (pr-str (->> @ingredients
                                                                      (filter #(contains? @selected-ingredients (first %)))
                                                                      (map second)))}))
                                 (.then #(.text %))
                                 (.then #(do
                                           (reset! loading false)
                                           (open-tab (str "https://trello.com/c/" %))))))))}
           [:div.flex.items-center
            (if @loading
              [:div {:style {:width 128}}
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
                  [:animateTransform {:attributeName "transform" :type "rotate" :repeatCount "indefinite" :dur "1s" :values "0 50 50;-45 50 50;0 50 50" :keyTimes "0;0.5;1"}]]]]]
              [:<>
               [:span.f2.mr2 "Fertig"]
               [:span.w2.h2.pt1 [icon {:color "white"} :check-mark]]])]]])])))


(dom/render [app] (.getElementById js/document "app"))
