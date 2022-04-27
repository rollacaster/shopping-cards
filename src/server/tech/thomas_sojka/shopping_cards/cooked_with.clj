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

(defn edit [conn cooked-with-id cooked-with-update]
  (let [{:keys [amount unit amount-desc]} cooked-with-update
        new-cooked-with (cond-> {:db/id [:cooked-with/id cooked-with-id]}
                          amount-desc (assoc :cooked-with/amount-desc amount-desc)
                          amount (assoc :cooked-with/amount amount)
                          unit (assoc :cooked-with/unit unit))]
    (db/transact conn [new-cooked-with])
    {:status 200 :body new-cooked-with}))

(defn delete [conn cooked-with-id]
  (db/retract conn [:cooked-with/id cooked-with-id])
  {:status 200})
