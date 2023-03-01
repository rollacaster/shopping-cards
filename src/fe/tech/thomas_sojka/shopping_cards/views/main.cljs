(ns tech.thomas-sojka.shopping-cards.views.main
  (:require ["firebase/auth" :as auth]
            [cljs.core :as c]
            [re-frame.core :refer [subscribe]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.auth :refer [auth user]]
            [tech.thomas-sojka.shopping-cards.components :as components]))

(defmulti title :view)
(defmethod title :default [] nil)

(defonce menu-visible? (r/atom false))
(def toggle-menu (fn []
                   (js/document.body.classList.toggle "overflow-hidden")
                   (swap! menu-visible? not)))
(defn- menu-button []
  [:button.bg-transparent.white.f2.bn.pa0.ma0.w2.h2
      {:on-click toggle-menu :style {:transform "translateY(-2px)"}}
      [components/icon {:color "white"} :menu]])

(defn header [{:keys [title]}]
  [:header.bg-orange-400.z-3
   [:div.mw9.center
    [:div.pv3.ph5-ns.ph3.flex.gap-8.w-100.items-center {:style {:gap 12}}
     [menu-button]
     [:h1.ma0.gray-800.ml2-ns.truncate
      title]]]])

(defn- nav-link [{:keys [link title on-click active? class]}]
  [:li.flex.justify-center {:class class}
   [:a.link.ph4.pv3.w-100.tc
    {:href link :on-click on-click :class (if active? "bg-orange-400 white" "bg-orange-100 gray-900")}
    title]])

(defn menu [{:keys [shopping-entries? route-name]}]
  [:div.absolute.left0.top0.h-100.z-2.flex.w-100
   {:style
    {:left (if @menu-visible? "0%" "-100%")
     :transition "all 300ms"}}
   [:div.bg-orange-400.shadow-1
    {:class "h-100"
     :style {:margin-top 68.8}}
    [:nav
     [:ul.list.pl0.ma0
      [nav-link {:link (rfe/href :route/main)
                 :title "Essensplan"
                 :active? (= route-name :route/main)
                 :on-click toggle-menu}]
      (when shopping-entries?
        [nav-link {:link (rfe/href :route/shoppping-list)
                   :title "Einkaufsliste"
                   :active? (= route-name :route/shoppping-list)
                   :on-click toggle-menu}])
      [nav-link {:link (rfe/href :route/edit-recipes)
                 :title "Rezepte"
                 :active? (= route-name :route/edit-recipes)
                 :on-click toggle-menu}]
      [nav-link {:link (rfe/href :route/ingredients)
                 :title "Zutaten"
                 :active? (= route-name :route/ingredients)
                 :on-click toggle-menu}]
      [nav-link {:title "Logout"
                 :on-click (fn []
                             (auth/signOut auth)
                             (toggle-menu))}]]]]
   [:button.bg-transparent.bn
    {:class "flex-auto"
     :on-click toggle-menu
     :style {:margin-top 68.8 :height "calc(100% - 68.8px)"}}]])

(defn app [match]
  (let [route @match
        error @(subscribe [:app/error])
        shopping-entries? @(subscribe [:shopping-entries?])
        route-name (:name (:data route))]
    [:div.sans-serif.flex.flex-column.h-100
     [header {:title (:title (:data route))}]
     [menu {:route-name route-name :shopping-entries? shopping-entries?}]
     [:main.flex-auto
      {:style {:margin-bottom 50}}
      [:div.mw9.center.bg-gray-200.h-100
       (case @user
         :loading [:div.flex.justify-center.items-center.h-100
                   [components/spinner]]
         :noauth [(:view (:data route)) route]
         [(:view (:data route)) route])]]
     (when (and shopping-entries? (or (= route-name :route/main) (= route-name :route/shoppping-list)))
       [:nav.fixed.bottom-0.w-100
        [:ul.flex.list.pl0.ma0

         [nav-link {:link (rfe/href :route/main)
                    :title "Essensplan"
                    :active? (= route-name :route/main)
                    :class "w-50"}]
         [nav-link {:link (rfe/href :route/shoppping-list)
                    :title "Einkaufsliste"
                    :active? (= route-name :route/shoppping-list)
                    :class "w-50"}]]])
     (when error
       [:div.absolute.white.flex.justify-center.w-100.mb4
        {:style {:bottom "3rem"}}
        [:div.w-80.bg-light-red.ph3.pv2.br2.ba.b--white
         error]])]))
