(ns tech.thomas-sojka.shopping-cards.core
  (:require [day8.re-frame.http-fx]
            [re-frame.core :refer [clear-subscription-cache! dispatch-sync]]
            [reagent.dom :as dom]
            [tech.thomas-sojka.shopping-cards.app]
            [tech.thomas-sojka.shopping-cards.auth :as auth]
            [tech.thomas-sojka.shopping-cards.bank-holidays]
            [tech.thomas-sojka.shopping-cards.dev-utils :as dev-utils]
            [tech.thomas-sojka.shopping-cards.firestore :as firestore]
            [tech.thomas-sojka.shopping-cards.ingredients]
            [tech.thomas-sojka.shopping-cards.meal-plans]
            [tech.thomas-sojka.shopping-cards.recipes]
            [tech.thomas-sojka.shopping-cards.router :as router]
            [tech.thomas-sojka.shopping-cards.shopping-items]
            [tech.thomas-sojka.shopping-cards.views.main :as main]))

(defn init!
  ([]
   (init! {:container (.getElementById js/document "app")}))
  ([{:keys [container]}]
   (let [now (js/Date.)]
     (when-not @dev-utils/restarting
       (firestore/init)
       (dispatch-sync [:app/init now])
       (auth/user-sync)))
   (dom/render [main/app (router/init)] container)))

(defn ^:dev/after-load clear-cache-and-render!
  []
  (reset! dev-utils/restarting true)
  (clear-subscription-cache!)
  (init!)
  (reset! dev-utils/restarting false))
