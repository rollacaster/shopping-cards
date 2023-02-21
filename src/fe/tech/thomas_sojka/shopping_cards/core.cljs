(ns tech.thomas-sojka.shopping-cards.core
  (:require [day8.re-frame.http-fx]
            [re-frame.core :refer [clear-subscription-cache! dispatch-sync
                                   subscribe]]
            [reagent.dom :as dom]
            [tech.thomas-sojka.shopping-cards.app]
            [tech.thomas-sojka.shopping-cards.bank-holidays]
            [tech.thomas-sojka.shopping-cards.firestore]
            [tech.thomas-sojka.shopping-cards.meal-plans]
            [tech.thomas-sojka.shopping-cards.recipes]
            [tech.thomas-sojka.shopping-cards.router :as router]
            [tech.thomas-sojka.shopping-cards.shopping-items]
            [tech.thomas-sojka.shopping-cards.views.main :as main]))

(defn init!
  ([]
   (init! {:container (.getElementById js/document "app")}))
  ([{:keys [container]}]
   (when (empty? @(subscribe [:recipes]))
     (dispatch-sync [:app/init (js/Date.)]))
   (dom/render [main/app (router/init)] container)))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (clear-subscription-cache!)
  (init!))
