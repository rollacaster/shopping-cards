(ns tech.thomas-sojka.shopping-cards.recipe
  (:require
   [clojure.string :as str]
   [datomic.client.api :as d]
   [tech.thomas-sojka.shopping-cards.db :as db]))

(defn load-by-id [conn recipe-id]
  (d/pull
   (d/db conn)
   [[:recipe/id]
    [:recipe/name]
    [:recipe/image]
    [:recipe/link]
    {[:recipe/type] [[:db/ident]]}
    {[:cooked-with/_recipe]
     [[:cooked-with/id]
      [:cooked-with/amount]
      [:cooked-with/unit]
      [:cooked-with/amount-desc]
      {[:cooked-with/ingredient]
       [[:ingredient/name]
        [:ingredient/id]]}]}]
   [:recipe/id recipe-id]))

(defn edit [conn recipe-id recipe-upddate]
  (let [{:keys [type]} recipe-upddate
        new-recipe (cond-> {:db/id [:recipe/id recipe-id]}
                     type (assoc :recipe/type (keyword (str "recipe-type/" (str/lower-case type)))))]
    (db/transact conn [new-recipe])
    {:status 200 :body new-recipe}))
