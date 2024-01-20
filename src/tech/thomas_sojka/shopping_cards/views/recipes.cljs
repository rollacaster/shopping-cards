(ns tech.thomas-sojka.shopping-cards.views.recipes
  (:require [clojure.string :as str]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn recipe [{:keys [even name image selected? details-link]}]
  [:a.no-underline.relative.w-100.w-auto-ns.flex.db-ns.tl.outline-transparent.bg-gray-600-ns.pa0.bt-0.br-0.bl-0.bb-0-ns.bb.b--gray-900.bw1.ml3-ns.mb3-ns.br2-ns.h3.h-auto-ns
   {:href details-link :class (if even "bg-gray-600 white" "bg-gray-300 gray-900")}
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

(defn main []
  (let [filter-value (r/atom "")]
    (fn []
      (let [recipes @(subscribe [:recipes])]
        [:<>
         [c/search-filter {:value @filter-value
                           :on-change (fn [event] (reset! filter-value ^js (.-target.value event)))}]
         [:div.pb6.bg-gray-300
          (doall
           (->> recipes
                (sort-by :recipe/name)
                (filter (fn [{:recipe/keys [name]}]
                          (str/includes? (str/lower-case name)
                                         (str/lower-case @filter-value))))
                (map-indexed
                 (fn [idx {:recipe/keys [id name image]}]
                   ^{:key id}
                   [recipe {:name name :image image :details-link (rfe/href :route/edit-recipe {:recipe-id id})
                            :even (even? idx)}]))))]]))))
