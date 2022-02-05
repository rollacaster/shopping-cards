(ns tech.thomas-sojka.shopping-cards.events
  (:require
   [ajax.core :as ajax]
   [cljs.spec.alpha :as s]
   [re-frame.core
    :refer [after
            dispatch
            reg-event-db
            reg-event-fx
            reg-fx
            reg-global-interceptor]]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]
   [tech.thomas-sojka.shopping-cards.db :refer [default-db]]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (prn (s/explain-str a-spec db))
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :app/db)))
(reg-global-interceptor check-spec-interceptor)
(reg-fx :app/push-state
        (fn [route]
          (apply rfe/push-state route)))

(reg-event-db
 :app/navigate
 (fn [db [_ match]]
   (assoc db :app/route match)))

(reg-fx :app/scroll-to
        (fn [[x y]]
          (.scrollTo js/window x y)))

(reg-event-db
 :app/remove-error
 (fn [db] (assoc db :app/error nil)))

(reg-event-fx
 :app/initialise
 (fn [_ [_ year]]
   {:db default-db
    :http-xhrio {:method :get
                 :uri (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                           year
                           ".edn")
                 :response-format (ajax/text-response-format)
                 :on-success [:main/success-bank-holidays]
                 :on-failure [:main/failure-bank-holidays]}}))

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




