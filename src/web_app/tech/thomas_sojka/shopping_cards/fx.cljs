(ns tech.thomas-sojka.shopping-cards.fx
  (:require
   [datascript.core :as d]
   [re-frame.core :refer [dispatch reg-fx subscribe]]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]))

(reg-fx :app/push-state
        (fn [route]
          (apply rfe/push-state route)))

(reg-fx :app/scroll-to
        (fn [[x y]]
          (.scrollTo js/window x y)))

(reg-fx :db/datascript
        (fn [[type params]]
          (case type
            :schema
            (dispatch [:db/conn (d/create-conn params)])
            :query
            (dispatch
             (conj (:on-success params)
                   (if (:params params)
                     (d/q (:q params)
                          (deref @(subscribe [:db/conn]))
                          (:params params))
                     (d/q (:q params)
                          (deref @(subscribe [:db/conn]))))))
            :transact
            (if (:on-success params)
              (dispatch
               (conj (:on-success params)
                     (d/transact!
                      @(subscribe [:db/conn])
                      (:tx-dat params))))
              (d/transact!
               @(subscribe [:db/conn])
               (:tx-data params))))))

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
