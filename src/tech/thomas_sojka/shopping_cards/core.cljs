(ns tech.thomas-sojka.shopping-cards.core
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as s]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [reitit.frontend :as rf]
            [reitit.coercion.spec :as rss]
            [reitit.frontend.easy :as rfe]))

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"})

(defn icon
  ([name]
   [icon {} name])
  ([_ name]
   [:svg {:viewBox "0 0 24 24"}
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

(def recipes (r/atom []))
(defonce selected-recipes (r/atom #{}))
(def selected-ingredients (r/atom #{}))
(def ingredients (r/atom []))
(def loading (r/atom false))
(def type-order ["NORMAL" "FAST" "RARE"])

(defn select-recipes []
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
             [recipe (let [selected? (contains? @selected-recipes id)]
                       {:key id
                        :even (even? idx)
                        :name name
                        :link link
                        :image image
                        :selected? selected?
                        :on-click #(swap! selected-recipes
                                          (fn [selected-recipes]
                                            ((if selected? disj conj)
                                             selected-recipes id)))})])
           recipes))]])
     (->> @recipes
          (group-by :type)
          (sort-by
           (fn [[recipe-type]] (->> type-order
                                   (map-indexed #(vector %1 %2))
                                   (some
                                    (fn [[idx recipe-type-order]]
                                      (when (= recipe-type-order recipe-type) idx))))))
          (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)])))))])

(defn show-recipes []
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
                              :on-click #(rfe/push-state ::recipe {:recipe-id id})}]))))])
     (->> @recipes
          (group-by :type)
          (sort-by
           (fn [[recipe-type]] (some (fn [[idx recipe-type-order]] (when (= recipe-type-order recipe-type) idx))
                                    (map-indexed #(vector %1 %2) type-order))))
          (map (fn [[recipe-type recipes]] [recipe-type (sort-by :name recipes)])))))])


(defn show-recipe [{{{:keys [recipe-id]} :path}:parameters}]
  (let [ingredients (r/atom [])]
    (-> (.fetch js/window (str "/recipes/" recipe-id "/ingredients"))
        (.then #(.text %))
        (.then read-string)
        (.then #(reset! ingredients %)))
    (fn [match]
      (let [{:keys [path]} (:parameters match)
            {:keys [recipe-id]} path
            {:keys [name link image]}
            (->> @recipes
                 (some #(when (= (:id %) recipe-id) %)))]
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
            @ingredients)]]
         [:iframe.w-100 {:src link :style {:height "50rem"}}]]))))
(defn select-water [ingredients]
  (conj ingredients ["6175d1a2-0af7-43fb-8a53-212af7b72c9c"
                                              "Wasser"]))
(defn deselect-ingredients []
  (-> (.fetch js/window (str "/ingredients?" (s/join "&" (map #(str "recipe-ids=" %) @selected-recipes))))
      (.then #(.text %))
      (.then read-string)
      (.then #(reset! ingredients (select-water %))))
  (fn []
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
                   @ingredients))]))

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
    [:footer.fixed.bottom-0.w-100.bg-orange-400.flex.justify-center.pa3.z-2
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
    [:div.sans-serif.h-100
     [header]
     [:main.h-100
      [:div.mw9.center.bg-gray-200.h-100
       [(:view (:data @match)) @match]]]
     [footer]]))
(defn add-water [ingredients]
  (conj ingredients "6175d1a2-0af7-43fb-8a53-212af7b72c9c"))
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
                                 (reset! selected-ingredients (add-water (set (map first %))))
                                 (.scrollTo js/window 0 0)))))}]
   ["/show-recipes"
    {:name ::recipes
     :view show-recipes
     :title "Rezepte"}]
   ["/show-recipes/:recipe-id"
    {:name ::recipe
     :view show-recipe
     :title "Rezept"
     :parameters {:path {:recipe-id string?}}}]
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
