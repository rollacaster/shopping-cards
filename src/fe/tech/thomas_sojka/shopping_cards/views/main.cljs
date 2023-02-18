(ns tech.thomas-sojka.shopping-cards.views.main
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.auth :refer [user]]
            [tech.thomas-sojka.shopping-cards.components :as components]))

(defmulti title :view)
(defmethod title :default [] nil)

(defn header [{:keys [title]}]
  [:header.bg-orange-400
   [:div.mw9.center
    [:div.pv3.ph5-ns.ph3.flex.justify-between.w-100.items-center
     [:h1.ma0.gray-800.ml2-ns.truncate
      title]]]])

(defn app []
  (let [route @(subscribe [:app/route])
        error @(subscribe [:app/error])]
    [:div.sans-serif.flex.flex-column.h-100
     [header {:title (:title (:data route))}]
     [:main.flex-auto
      [:div.mw9.center.bg-gray-200.h-100
       (case @user
         :loading [:div.flex.justify-center.items-center.h-100
                   [components/spinner]]
         (if (not= @user :noauth)
           [(:view (:data route)) route]
           :view/login))]]
     (when error
       [:div.absolute.white.flex.justify-center.w-100.mb4
        {:style {:bottom "3rem"}}
        [:div.w-80.bg-light-red.ph3.pv2.br2.ba.b--white
         error]])]))
