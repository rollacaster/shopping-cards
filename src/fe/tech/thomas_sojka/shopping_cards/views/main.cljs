(ns tech.thomas-sojka.shopping-cards.views.main
  (:require [cljs.core :as c]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.auth :refer [user]]
            [tech.thomas-sojka.shopping-cards.components :as components]))

(defmulti title :view)
(defmethod title :default [] nil)

(defn header [{:keys [title toggle-menu]}]
  [:header.bg-orange-400.z-3
   [:div.mw9.center
    [:div.pv3.ph5-ns.ph2.flex.gap-8.w-100.items-center {:style {:gap 12}}
     [:button.bg-transparent.white.f2.bn.pa0.ma0.w2.h2
      {:on-click toggle-menu :style {:transform "translateY(-2px)"}}
      [components/icon {:color "white"} :menu]]
     [:h1.ma0.gray-800.ml2-ns.truncate
      title]]]])

(defn- nav-link [{:keys [link title toggle-menu active?]}]
  [:li.ph4.pv3
   {:class (when active? "bg-orange-500 white")}
   [:a.link {:href link :on-click toggle-menu
             :class (if active? "white" "gray-900")}
    title]])

(defn menu [{:keys [visible? toggle-menu match]}]
  [:div.absolute.left0.top0.h-100.z-2.flex
   {:style
    {:left (if visible? "0%" "-100%")
     :transition "all 300ms"}}
   [:div.bg-orange-400
    {:class "w-2/3"
     :style {:margin-top 68.8 :height "calc(100% - 68.8px)"}}
    [:nav
     [:ul.list.pl0.ma0
      [nav-link {:toggle-menu toggle-menu
                 :link (rfe/href :route/main)
                 :title "Home"
                 :active? (= (:name (:data @match)) :route/main)}]
      [nav-link {:toggle-menu toggle-menu
                 :link (rfe/href :route/shoppping-card)
                 :title "Einkaufsliste"
                 :active? (= (:name (:data @match)) :route/shoppping-card)}]]]]
   [:div {:on-click toggle-menu :class "w-1/3 h-full"}]])

(defn app []
  (let [menu-visible? (r/atom false)]
    (fn [match]
      (let [route @match
            error @(subscribe [:app/error])
            toggle-menu #(swap! menu-visible? not)]
        [:div.sans-serif.flex.flex-column.h-100
         [header {:title (:title (:data route)) :toggle-menu toggle-menu}]
         [menu {:visible? @menu-visible? :toggle-menu toggle-menu :match match}]
         [:main.flex-auto
          [:div.mw9.center.bg-gray-200.h-100
           (case @user
             :loading [:div.flex.justify-center.items-center.h-100
                       [components/spinner]]
             [(:view (:data route)) route])]]
         (when error
           [:div.absolute.white.flex.justify-center.w-100.mb4
            {:style {:bottom "3rem"}}
            [:div.w-80.bg-light-red.ph3.pv2.br2.ba.b--white
             error]])]))))
