(ns tech.thomas-sojka.shopping-cards.core
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as s]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [reitit.frontend :as rf]
            [reitit.coercion.spec :as rss]
            [reitit.frontend.easy :as rfe]))

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
    [:div.bg-gray-700.absolute.pa2.mh2.mb2.bottom-0.o-50.br2
     [:span.white.f4 name]]
    [:div.absolute.pa2.mh2.mb2.bottom-0
     [:span.white.f4 name]]]])

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

(def recipes (r/atom []))
(def selected-recipes (r/atom #{}))
(def selected-ingredients (r/atom #{}))
(def ingredients (r/atom []))
(def loading (r/atom false))

(defn select-recipes []
  [:div.flex.flex-wrap.justify-center.justify-start-ns.ph5.pb6.pt3
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
         @recipes))])

(defn deselect-ingredients []
  (-> (.fetch js/window (str "/ingredients?" (s/join "&" (map #(str "recipe-ids=" %) @selected-recipes))))
                       (.then #(.text %))
                       (.then read-string)
                       (.then #(reset! ingredients %)))
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
                 @ingredients))])

(defonce match (r/atom nil))

(defn header []
  [:header.bg-orange-400
   [:div.mw9.center
    [:div.pv3.ph5-ns.ph3
     [:h1.ma0.gray-800.ml2-ns
      (:title (:data @match))]]]])

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
  (when (and (> (count @selected-recipes) 0))
    [:footer.fixed.bottom-0.w-100.bg-orange-400.flex.justify-center.pa3
     [:button.br3.bg-gray-700.pointer.bn.shadow-3.ph3.pv2.white
      {:on-click (:action (:data @match))}
      [:div.flex.items-center
       (if @loading
         [:div {:style {:width 128}}
          [spinner]]
         [:<>
          [:span.f2.mr2 "Fertig"]
          [:span.w2.h2.pt1 [icon {:color "white"} :check-mark]]])]]]))

(defn app []
  (-> (.fetch js/window "/recipes")
      (.then #(.json %))
      (.then #(js->clj % :keywordize-keys true))
      (.then #(reset! recipes %)))
  (fn []
    [:div.sans-serif.h-100.bg-gray-200
     [header]
     [:main.h-100
      [:div.mw9.center
       [(:view (:data @match)) @match]]]
     [footer]]))

(def routes
  [["/" {:name ::main
         :view select-recipes
         :title "Rezepte"
         :action (fn []
                   (reset! loading true)
                   (-> (.fetch js/window (str "/ingredients?" (s/join "&" (map #(str "recipe-ids=" %) @selected-recipes))))
                       (.then #(.text %))
                       (.then read-string)
                       (.then #(do
                                 (reset! loading false)
                                 (rfe/push-state ::deselect-ingredients)
                                 (reset! ingredients %)
                                 (reset! selected-ingredients (set (map first %)))
                                 (.scrollTo js/window 0 0)))))}]
   ["/deselect-ingredients" {:name ::deselect-ingredients
                             :view deselect-ingredients
                             :title "Zutaten auswählen"
                             :action (fn []
                                       (reset! loading true)
                                       (-> (.fetch js/window "/shopping-card"
                                                   (clj->js {:method "POST"
                                                             :headers {"Content-type" "application/edn"}
                                                             :body (pr-str (->> @ingredients
                                                                                (filter #(contains? @selected-ingredients (first %)))
                                                                                (map second)))}))
                                           (.then #(.text %))
                                           (.then #(do
                                                     (reset! loading false)
                                                     (rfe/push-state ::finish {:card-id %})))))}]
   ["/finish/:card-id" {:name ::finish
               :view finish
               :title "Einkaufszettel erstellt"
               :parameters {:path {:card-id string?}}
               :action (fn []
                         (reset! selected-ingredients #{})
                         (rfe/push-state ::main))}]])

(defn init! []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (reset! match m))
   {:use-fragment true})
  (dom/render [app] (.getElementById js/document "app")))

(init!)
