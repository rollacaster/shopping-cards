(ns tech.thomas-sojka.shopping-cards.view
  (:require [re-frame.core :refer [subscribe]]))

(defmulti title :view)
(defmethod title :default [] nil)

(defn header []
  (let [route @(subscribe [:app/route])]
    [:header.bg-orange-400
     [:div.mw9.center
      [:div.pv3.ph5-ns.ph3.flex.justify-between.w-100.items-center
       [:h1.ma0.gray-800.ml2-ns.truncate
        [title {:view (:view (:data route))}]]]]]))

(defmulti content :view)
(defmethod content :default [] nil)

(defn app []
  (fn []
    (let [route @(subscribe [:app/route])
          error @(subscribe [:app/error])]
      [:div.sans-serif.flex.flex-column.h-100
       [header]
       [:main.flex-auto
        [:div.mw9.center.bg-gray-200.h-100
         [content {:view (:view (:data route))}]]]
       (when error
         [:div.absolute.white.bottom-0.flex.justify-center.w-100.mb4
          [:div.w-80.bg-light-red.ph3.pv2.br2.ba.b--white
           error]])])))
