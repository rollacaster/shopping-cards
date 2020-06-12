(ns tech.thomas-sojka.ingredients.main
  (:require [ring.adapter.jetty :refer [run-jetty]]
            [tech.thomas-sojka.ingredients.handler :refer [app]]))

(defonce server (atom nil))

(defn -main []
    (reset! server
          (run-jetty app {:port 3000 :join? false})))

(defn restart-server []
  (when @server
    (.stop @server))
  (-main))

(comment
  (restart-server))