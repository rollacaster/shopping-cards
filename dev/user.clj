(ns user
  (:require [integrant.repl :as ig-repl]
            [integrant.core :as ig]
            [tech.thomas-sojka.shopping-cards.system :as system]))

(ig-repl/set-prep! (fn [] system/config))

(defmethod ig/init-key :external/trello-client [_ _]
  {:create-klaka-shopping-card (fn [ingredients]
                                 (prn "New Card" ingredients)
                                 "mock")})
(ig-repl/go)
(comment
  (ig-repl/go)
  (ig-repl/halt)
  (ig-repl/reset))

