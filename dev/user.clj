(ns user
  (:require [integrant.core :as ig]
            [integrant.repl :as ig-repl]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]
            [tech.thomas-sojka.shopping-cards.system :as system]))

(ig-repl/set-prep! (fn [] system/config))

(defmethod ig/init-key :external/trello-client [_ _]
  {:create-klaka-shopping-card (fn [ingredients]
                                 (prn "New Card" ingredients)
                                 "mock")})

(defn cljs-repl
  "Connects to a given build-id. Defaults to `:app`."
  ([]
   (cljs-repl :app))
  ([build-id]
   (server/start!)
   (shadow/watch build-id)
   (shadow/nrepl-select build-id)))

(ig-repl/go)
(comment
  (ig-repl/go)
  (ig-repl/halt)
  (ig-repl/reset))

