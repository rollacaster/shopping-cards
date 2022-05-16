(ns tech.thomas-sojka.shopping-cards.fx
  (:require
   [datascript.core :as d]
   [re-frame.core :refer [dispatch reg-fx]]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tech.thomas-sojka.shopping-cards.db :refer [conn]]))

(reg-fx :app/push-state
        (fn [route]
          (apply rfe/push-state route)))

(reg-fx :app/scroll-to
        (fn [[x y]]
          (.scrollTo js/window x y)))

(reg-fx :db/datascript
        (fn [[type params]]
          (case type
            :query
            (dispatch
             (conj (:on-success params)
                   (if (:params params)
                     (d/q (:q params)
                          @conn
                          (:params params))
                     (d/q (:q params)
                          @conn))))
            :transact
            (dispatch
             (conj (:on-success params)
                   (d/transact!
                    conn
                    (:tx-dat params)))))))

(defonce timeouts (r/atom {}))

(reg-fx
 :app/timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @timeouts id)]
     (js/clearTimeout existing)
     (swap! timeouts dissoc id))
   (when (some? event)
     (swap! timeouts assoc id
            (js/setTimeout
             (fn []
               (dispatch event))
             time)))))
