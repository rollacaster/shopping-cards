(ns tech.thomas-sojka.ingredients.core
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as s]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"})

(defn icon [name]
  [:svg {:viewBox "0 0 24 24"}
   [:path {:d (icons name)}]])

(defn recipe [{:keys [name image selected? on-click]}]
  [:button.bn.bg-transparent.outline-transparent {:on-click on-click}
   [:div.w5.h5.relative.ma2.shadow-3 {:class (when selected? "o-50")}
    (when selected?
      [:div.w4.h4.absolute {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
       [icon :check-mark]])
    [:img.br2.w-100.h-100 {:style {:object-fit "cover"} :src image}]
    [:div.bg-dark-gray.absolute.pa2.mh2.mb2.bottom-0.o-50.br2
     [:span.white.f4 name]]
    [:div.absolute.pa2.mh2.mb2.bottom-0
     [:span.white.f4 name]]]])

(defn ingredient [{:keys [i id selected? on-change]} children]
  [:li.flex.items-center.ph4.pv3 {:class (if (= (mod i 2) 0) "bg-light-gray near-black" "bg-gray white")}
   [:input.pointer.mh2
    {:id id :type "checkbox" :checked selected? :on-change on-change}]
   [:label.pointer.f4 {:for id} children]])

(def recipes (r/atom []))
(def selected-recipes (r/atom #{"dd3fa340-a54a-4dc8-aea2-68cdc3656608"}))
(def selected-ingredients (r/atom #{}))
(def ingredients (r/atom []))

(defn app []
  (let [step (r/atom "SELECT_RECIPE")]
    (-> (.fetch js/window "http://localhost:3000/recipes")
        (.then #(.json %))
        (.then #(js->clj % :keywordize-keys true))
        (.then #(reset! recipes %)))
    (fn []
      [:div.sans-serif
       [:header.bg-dark-gray.white.pv3.ph4
        [:div.ml2 [:h1.ma0
                   (case @step
                     "SELECT_RECIPE" "Select Recipes"
                     "DESELECT_INGREDIENTS" "Remove available ingredients"
                     "TODO")]]]
       [:main
        (case @step
          "SELECT_RECIPE"
          [:div.flex.flex-wrap.justify-center.justify-start-ns.ph4.pb6.mt3
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
          [:ul.list.pl0.mv0
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
          "TODO")]
       (when (and (> (count @selected-recipes) 0))
         [:footer.fixed.bottom-0.w-100.bg-dark-gray.flex.justify-center.pa3
          [:button.br2.bg-light-gray.pointer
           {:on-click (fn []
                        (case @step
                          "SELECT_RECIPE"
                          (-> (.fetch js/window (str "http://localhost:3000/ingredients?"
                                                    (s/join "&" (map #(str "recipe-ids=" %) @selected-recipes))))
                              (.then #(.text %))
                              (.then read-string)
                              (.then #(do
                                        (reset! step "DESELECT_INGREDIENTS")
                                        (reset! ingredients %)
                                        (reset! selected-ingredients (set (map first %))))))
                          "DESELECT_INGREDIENTS"
                          (-> (.fetch js/window "http://localhost:3000/shopping-card"
                                      (clj->js {:method "POST"
                                                :headers {"Content-type" "application/edn"}
                                                :body (pr-str (->> @ingredients
                                                                   (filter #(contains? @selected-ingredients (first %)))
                                                                   (map second)))}))
                              (.then #(.text %))
                              (.then #(.open js/window (str "https://trello.com/c/" %) "_blank")))))}
           [:div.flex.items-center
            [:span.f2.mr2 "Fertig"]
            [:span.w2.h2 [icon :check-mark]]]]])])))


(dom/render [app] (.getElementById js/document "app"))
