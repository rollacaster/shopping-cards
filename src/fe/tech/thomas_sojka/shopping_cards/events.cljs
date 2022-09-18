(ns tech.thomas-sojka.shopping-cards.events
  (:require
   [ajax.core :as ajax]
   [cljs.reader :refer [read-string]]
   [cljs.spec.alpha :as s]
   [datascript.core :as d]
   [re-frame.core
    :refer [after
            reg-event-db
            reg-event-fx
            reg-global-interceptor]]
   [tech.thomas-sojka.shopping-cards.db :refer [default-db]]))

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

;; TODO move to cofx
(def conn (atom nil))

(-> (js/fetch "./datascript-export.edn" )
      (.then (fn [res] (.text res)))
      (.then (fn [res] (reset! conn (read-string res))))
      (.catch js/console.log))

(reg-event-fx :query
  (fn [{:keys [db]} [_ {:keys [q params on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :dispatch (conj
                on-success
                (if params
                  (d/q q @conn params)
                  (d/q q @conn)))}))

(reg-event-fx :query/log
  (fn [_ [_ props]]
    {:dispatch [:query
                (assoc props
                       :on-success [:log] :on-failure [:log])]}))

(reg-event-fx :log
  (fn [_ [_ d]]
    (js/console.log d)))

(reg-event-fx :transact
  (fn [{:keys [db]} [_ {:keys [tx-data on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :http-xhrio {:method :put
                  :uri "/transact"
                  :params tx-data
                  :format (ajax/transit-request-format)
                  :response-format (ajax/raw-response-format)
                  :on-success on-success
                  :on-failure on-failure}}))
