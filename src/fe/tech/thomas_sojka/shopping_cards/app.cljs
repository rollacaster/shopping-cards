(ns tech.thomas-sojka.shopping-cards.app
  (:require ["date-fns" :refer [startOfDay]]
            [cljs.spec.alpha :as s]
            [expound.alpha :as expound]
            [re-frame.core :refer [after dispatch reg-event-db reg-event-fx
                                   reg-fx reg-global-interceptor reg-sub]]
            [reagent.core :as r]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.db :refer [default-db]]))

(reg-event-fx :app/init
 (fn [_ [_ now]]
   {:db (assoc default-db :app/start-of-week (startOfDay now))
    :dispatch-n [[:bank-holidays/load (.getFullYear now)]
                 [:recipes/load]
                 [:meals/load now]
                 [:ingredients/load]
                 [:shopping-entry/load]]}))

(reg-event-fx :app/start-of-week
 (fn [{:keys [db]} [_ start]]
   {:db (assoc db :app/start-of-week start)}))

(reg-event-db :app/remove-error
 (fn [db] (assoc db :app/error nil)))

(reg-sub :app/error
 (fn [db _]
   (:app/error db)))

(reg-sub :app/route
 (fn [db _]
   (:app/route db)))

(reg-sub :app/loading
 (fn [db _]
   (:app/loading db)))

(reg-sub :app/start-of-week
 (fn [db _]
   (:app/start-of-week db)))

(reg-fx :app/push-state
  (fn [route] (apply rfe/push-state route)))

(reg-fx :app/scroll-to
  (fn [[x y]]
    (.scrollTo js/window x y)))

(defonce timeouts (r/atom {}))

(reg-fx :app/timeout
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

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (expound/expound a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :app/db)))
(reg-global-interceptor check-spec-interceptor)
