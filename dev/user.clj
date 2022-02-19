(ns user
  (:require [integrant.repl :as ig-repl]
            [tech.thomas-sojka.shopping-cards.system :as system]))

(ig-repl/set-prep! (fn [] system/config))

(comment
  (ig-repl/go)
  (ig-repl/halt)
  (ig-repl/reset))

