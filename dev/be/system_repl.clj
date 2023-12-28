(ns system-repl
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [nrepl.cmdline :as nrepl]
            [tech.thomas-sojka.shopping-cards.system :as system]))

(ig-repl/set-prep! (fn [] system/config))

(defmethod ig/init-key :external/trello-client [_ _]
  {:create-klaka-shopping-card (fn [ingredients]
                                 (prn "New Card" ingredients)
                                 "mock")})

(defn -main [& args]
  (set! *print-namespace-maps* false)
  (ig-repl/go)
  (apply nrepl/-main args))

(comment
  (ig-repl/go)
  (ig-repl/halt)
  (ig-repl/reset))
