(ns tech.thomas-sojka.shopping-cards.system
  (:gen-class)
  (:require [datomic.client.api :as d]
            [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]]
            [tech.thomas-sojka.shopping-cards.handler :refer [app]]
            [tech.thomas-sojka.shopping-cards.trello :as trello]))

(def config
  {:adapter/jetty {:port 3000
                   :trello-client (ig/ref :external/trello-client)
                   :conn (ig/ref :datomic/dev-local)}
   :external/trello-client {}
   :datomic/dev-local {:db-name "shopping-cards"}})

(defmethod ig/init-key :datomic/dev-local [_ {:keys [db-name]}]
  (let [client (d/client {:server-type :dev-local :system "dev"})
        conn (d/connect client {:db-name db-name})]
    conn))

(defmethod ig/init-key :adapter/jetty [_ {:keys [trello-client port conn]}]
  (run-jetty (app {:trello-client trello-client :conn conn}) {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :external/trello-client [_ _]
  {:create-klaka-shopping-card trello/create-klaka-shopping-card})

(defn -main []
  (ig/init config))
