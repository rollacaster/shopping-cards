(ns tech.thomas-sojka.shopping-cards.cooked-with
  (:require
    [tech.thomas-sojka.shopping-cards.db :as db]))

(defn create [conn recipe-id ingredient-id]
  (let [cooked-with {:cooked-with/id (str (random-uuid))
                     :cooked-with/recipe [:recipe/id recipe-id]
                     :cooked-with/ingredient [:ingredient/id ingredient-id]
                     :cooked-with/amount-desc "1"
                     :cooked-with/amount 1.0}]
    (db/transact conn [cooked-with])
    {:status 200 :body cooked-with}))

(defn delete [conn cooked-with-id]
  (db/retract conn [:cooked-with/id cooked-with-id])
  {:status 200})
