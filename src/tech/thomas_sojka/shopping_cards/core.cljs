(ns tech.thomas-sojka.shopping-cards.core
  (:require [re-frame.core
             :refer
             [clear-subscription-cache! dispatch dispatch-sync]]
            [reagent.dom :as dom]
            [reitit.coercion.spec :as rss]
            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.view :refer [app routes]]
            [day8.re-frame.http-fx]
            [tech.thomas-sojka.shopping-cards.events]
            [tech.thomas-sojka.shopping-cards.subs]))

(defn init! []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m]
     (dispatch [:navigate m]))
   {:use-fragment true})
  (dom/render [app] (.getElementById js/document "app")))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (clear-subscription-cache!)
  (init!))

(defonce start-up (do (dispatch-sync [:initialise-db]) true))

(init!)
