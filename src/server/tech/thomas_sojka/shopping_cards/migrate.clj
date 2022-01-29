(ns tech.thomas-sojka.shopping-cards.migrate
  (:require [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [tech.thomas-sojka.shopping-cards.schema :refer [meta-db]]))

(defn migrate-schema [conn]
  (let [datomic-schema (hodur-datomic/schema meta-db)]
    (d/transact conn {:tx-data datomic-schema})
    (d/transact conn {:tx-data [{:db/ident :cooked-with/recipe+ingredient
                                 :db/valueType :db.type/tuple
                                 :db/tupleAttrs [:cooked-with/ingredient :cooked-with/recipe]
                                 :db/cardinality :db.cardinality/one
                                 :db/unique :db.unique/identity}]})
    (d/transact conn {:tx-data [{:db/ident :meal-plan/inst+type
                                 :db/valueType :db.type/tuple
                                 :db/tupleAttrs [:meal-plan/inst :meal-plan/type]
                                 :db/cardinality :db.cardinality/one
                                 :db/unique :db.unique/identity}]})))
(comment
  (migrate-schema (d/connect (d/client {:server-type :dev-local
                          :system "dev"}) {:db-name "shopping-cards"})))
