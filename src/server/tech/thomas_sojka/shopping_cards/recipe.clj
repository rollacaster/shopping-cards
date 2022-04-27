(ns tech.thomas-sojka.shopping-cards.recipe
  (:require
   [clojure.string :as str]
   [datomic.client.api :as d]
   [tech.thomas-sojka.shopping-cards.db :as db]))

(defn load-by-id [conn recipe-id]
  (d/pull
   (d/db conn)
   [[:recipe/id :as :id]
    [:recipe/name :as :name]
    [:recipe/image :as :image]
    [:recipe/link :as :link]
    {[:recipe/type :as :type] [[:db/ident :as :type]]}
    {[:cooked-with/_recipe :as :cooked-with]
     [[:cooked-with/id :as :id]
      [:cooked-with/amount :as :amount]
      [:cooked-with/unit :as :unit]
      [:cooked-with/amount-desc :as :amount-desc]
      {[:cooked-with/ingredient :as :ingredient]
       [[:ingredient/name :as :name]]}]}]
   [:recipe/id recipe-id]))

(defn edit [conn recipe-id recipe-upddate]
  (let [{:keys [type]} recipe-upddate
        new-recipe (cond-> {:db/id [:recipe/id recipe-id]}
                     type (assoc :recipe/type (keyword (str "recipe-type/" (str/lower-case type)))))]
    (db/transact conn [new-recipe])
    {:status 200 :body new-recipe}))
