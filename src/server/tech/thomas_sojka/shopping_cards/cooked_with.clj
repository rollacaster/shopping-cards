(ns tech.thomas-sojka.shopping-cards.cooked-with
  (:require
    [tech.thomas-sojka.shopping-cards.db :as db]))

(defn delete [conn cooked-with-id]
  (db/retract conn [:cooked-with/id cooked-with-id])
  {:status 200})
