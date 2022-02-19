(ns tech.thomas-sojka.shopping-cards.system
  (:gen-class)
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]]
            [tech.thomas-sojka.shopping-cards.handler :refer [app]]))

(def config
  {:adapter/jetty {:port 3000}})

(defmethod ig/init-key :adapter/jetty [_ _]
  (run-jetty app {:port 3000 :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defn -main []
  (ig/init config))

