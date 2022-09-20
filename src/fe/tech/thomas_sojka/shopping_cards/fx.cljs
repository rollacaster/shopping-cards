(ns tech.thomas-sojka.shopping-cards.fx
  (:require
   [re-frame.core :refer [dispatch reg-fx reg-cofx]]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]))

(reg-fx :app/push-state
        (fn [route]
          (apply rfe/push-state route)))

(reg-fx :app/scroll-to
        (fn [[x y]]
          (.scrollTo js/window x y)))

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

(def conn (atom nil))

(reg-cofx :app/conn
  (fn [coeffects]
    (assoc coeffects :conn @conn)))

(reg-fx :app/set-conn
  (fn [connection]
    (reset! conn connection)))
