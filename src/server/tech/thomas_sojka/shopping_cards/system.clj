(ns tech.thomas-sojka.shopping-cards.system
  (:gen-class)
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]]
            [tech.thomas-sojka.shopping-cards.handler :refer [app]]
            [tech.thomas-sojka.shopping-cards.trello :as trello]))

(def config
  {:adapter/jetty {:port 3000
                   :trello-client (ig/ref :external/trello-client)}
   :external/trello-client {}})

(defmethod ig/init-key :adapter/jetty [_ {:keys [trello-client]}]
  (run-jetty (app {:trello-client trello-client}) {:port 3000 :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defmethod ig/init-key :external/trello-client [_ _]
  {:create-klaka-shopping-card trello/create-klaka-shopping-card})

(defn -main []
  (ig/init config))

