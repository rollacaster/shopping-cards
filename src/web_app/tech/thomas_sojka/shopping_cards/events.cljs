(ns tech.thomas-sojka.shopping-cards.events
  (:require
   [ajax.core :as ajax]
   [cljs.spec.alpha :as s]
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

(reg-event-fx :query
  (fn [{:keys [db]} [_ {:keys [q params on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :db/datascript [:query {:q q
                             :params params
                             :on-success on-success
                             :on-failure on-failure}]}))

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
     :db/datascript [:transact {:tx-data tx-data
                                :on-success on-success
                                :on-failure on-failure}]}))

(reg-event-fx :db/schema
  (fn []
    {:http-xhrio {:method :get
                  :uri "/schema"
                  :response-format (ajax/transit-response-format)
                  :on-success [:db/success-schema]
                  :on-failure [:db/failure-schema]}}))



(reg-event-fx :db/bootstrap
  (fn []
    {:http-xhrio {:method :get
                  :uri "/bootstrap"
                  :response-format (ajax/transit-response-format)
                  :on-success [:db/success-bootstrap]
                  :on-failure [:db/failure-bootstrap]}}))

(reg-event-fx :db/success-bootstrap
  (fn [_ [_ bootstrap-data]]
    {:dispatch-n [[:transact {:tx-data bootstrap-data}]
                  [:query
                   {:q '[:find (pull ?r [[:recipe/id :as :id]
                                         [:recipe/name :as :name]
                                         [:recipe/image :as :image]
                                         [:recipe/link :as :link]
                                         {:recipe/type [[:db/ident]]}])
                         :where
                         [?r :recipe/id ]]
                    :on-success [:main/success-recipes]
                    :on-failure [:main/failure-recipes]}]
                  #_[:main/init-meal-plans (js/Date.)]]}))

(reg-event-fx :db/success-schema
  (fn [_ [_ schema]]
    {:db/datascript [:schema schema]}))

(reg-event-db :db/conn
  (fn [db [_ conn]]
    (assoc db :db/conn conn)))
