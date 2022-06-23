(ns tech.thomas-sojka.shopping-cards.test-utils
  (:require
   [datomic.client.api :as d]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl]
   [tech.thomas-sojka.shopping-cards.fixtures :as fixtures]
   [tech.thomas-sojka.shopping-cards.db :as db]
   [tech.thomas-sojka.shopping-cards.system]))

(def db-name "shopping-cards-test")
(def port 3001)

(defn url [& endpoint]
  (apply str "http://localhost:" port endpoint))

(def config
  {:adapter/jetty {:port port
                   :trello-client (ig/ref :external/trello-client)
                   :conn (ig/ref :datomic/dev-local)}
   :external/trello-client {}
   :datomic/dev-local {:db-name db-name}})

(defn- populate []
  (let [client (d/client {:server-type :dev-local :system "dev"})
        conn (d/connect client {:db-name db-name})]
    (db/transact conn (concat fixtures/ingredients fixtures/recipes))
    (db/transact conn fixtures/cooked-with)))

(defn db-setup [test-run]
  (ig-repl/set-prep! (fn [] config))
  (ig-repl/go)
  (populate)
  (test-run)
  (d/delete-database (d/client {:server-type :dev-local :system "dev"})
                     {:db-name db-name})
  (ig-repl/halt))

(comment
  (do
    (ig-repl/halt)
    (ig-repl/go)
    (populate)))
