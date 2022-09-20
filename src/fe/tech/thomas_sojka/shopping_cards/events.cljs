(ns tech.thomas-sojka.shopping-cards.events
  (:require
   [ajax.core :as ajax]
   [cljs.reader :refer [read-string]]
   [cljs.spec.alpha :as s]
   [datascript.core :as d]
   [re-frame.core
    :refer [after
            reg-event-db
            inject-cofx
            reg-event-fx
            reg-global-interceptor]]
   [tech.thomas-sojka.shopping-cards.db :refer [default-db]]
   [tech.thomas-sojka.shopping-cards.queries :as queries]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (prn (s/explain-str a-spec db))
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :app/db)))
(reg-global-interceptor check-spec-interceptor)


(reg-event-db
 :app/navigate
 (fn [db [_ match]]
   (assoc db :app/route match)))

(reg-event-db
 :app/remove-error
 (fn [db] (assoc db :app/error nil)))

(reg-event-fx :app/init-holidays
 (fn [_ [_ year]]
   {:http-xhrio {:method :get
                 :uri (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                           year
                           ".edn")
                 :response-format (ajax/text-response-format)
                 :on-success [:main/success-bank-holidays]
                 :on-failure [:main/failure-bank-holidays]}}))

(reg-event-fx :app/success-init-db
  (fn [_ [_ now res]]
    {:app/set-conn (read-string res)
     :dispatch-n [[:query
                   {:q queries/load-recipes
                    :on-success [:main/success-recipes]
                    :on-failure [:main/failure-recipes]}]
                  [:main/init-meal-plans now]]}))

(reg-event-fx :app/failure-init-db
  (fn [{:keys [db]} _]
    {:db (assoc db :app/error "Fehler: Datenbank konnte nicht geladen werden")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :app/init-db
  (fn [_ [_ now]]
    {:http-xhrio {:method :get
                  :uri "./datascript-export.edn"
                  :response-format (ajax/text-response-format)
                  :on-success [:app/success-init-db now]
                  :on-failure [:app/failure-init-db]}}))

(reg-event-fx
 :app/initialise
 (fn [_ [_ now]]
   {:db default-db
    :dispatch-n [[:app/init-holidays (.getFullYear now)]
                 [:app/init-db now]]}))

(reg-event-fx :query
  [(inject-cofx :app/conn)]
  (fn [{:keys [conn db]} [_ {:keys [q params on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :dispatch (conj
                on-success
                (if params
                  (d/q q conn params)
                  (d/q q conn)))}))

(reg-event-fx :query/log
  (fn [_ [_ props]]
    {:dispatch [:query
                (assoc props
                       :on-success [:log] :on-failure [:log])]}))

(reg-event-fx :log
  (fn [_ [_ d]]
    (js/console.log d)))

(reg-event-fx :transact
  [(inject-cofx :app/conn)]
  (fn [{:keys [conn db]} [_ {:keys [tx-data on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :dispatch (conj on-success (d/transact! conn tx-data))}))
