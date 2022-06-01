(ns system-repl
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [tech.thomas-sojka.shopping-cards.system :as system]
            [nrepl.cmdline :as nrepl]))

(ig-repl/set-prep! (fn [] system/config))

(defmethod ig/init-key :external/trello-client [_ _]
  {:create-klaka-shopping-card (fn [ingredients]
                                 (prn "New Card" ingredients)
                                 "mock")})

(defn -main [& _args]
  (ig-repl/go)
  (nrepl/-main))

(comment
  (ig-repl/go)
  (ig-repl/halt)
  (ig-repl/reset))
