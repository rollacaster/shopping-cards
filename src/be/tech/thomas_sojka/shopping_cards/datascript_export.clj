(ns tech.thomas-sojka.shopping-cards.datascript-export
  (:require [datascript.core]
            [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [tech.thomas-sojka.shopping-cards.schema :as schema]))

(defn all-datams-seq [conn]
  (mapv
   (fn [[e a v tx added]]
     [e a v tx added])
   (d/datoms (d/db conn) {:index :eavt :limit -1})))

(defn- attr [datomic-datoms id]
    (let [[_ _ v] (first (filter (fn [[e]] (= e id)) datomic-datoms))]
      (assert (keyword? v))
      (case v
        :recipe/type :recipe/kind
        v)))

(defn- id-munging  [id]
    (if (> id 2147483647)
      (loop [n id]
        (if (< n 2147483647)
          n
          (recur (- n 2147483647))))
      id))

(defmacro datascript-schema []
    (->> schema/meta-db
         hodur-datomic/schema
         (filter (fn [{:db/keys [valueType cardinality unique]}]
                   (or valueType cardinality unique)))
         (mapv
          (fn [{:db/keys [ident valueType cardinality unique]}]
            (hash-map ident
                      (cond-> #:db{:cardinality cardinality }
                        (or (= valueType :db.type/ref)
                            (= valueType :db.type/tuple))
                        (assoc :db/valueType valueType)
                        unique (assoc :db/unique unique)))))
         (apply merge)))

(def conn (d/connect (d/client {:server-type :dev-local :system "dev"})
                       {:db-name "shopping-cards"}))

(defn enum-map [conn]
  (->> @schema/meta-db
       (datascript.core/q '[:find
                            (pull ?p [:type/kebab-case-name])
                            (pull ?e [:field/kebab-case-name])
                            :where
                            [?p :type/enum true]
                            [?e :field/parent ?p]])
       (map
        (fn [[parent child]]
          (let [[parent-keyword] (vals parent)
                [child-keyword] (vals child)]
            (keyword (name parent-keyword) (name child-keyword)))))
       (d/q
        '[:find  ?e ?enum-keyword
          :in $ [?enum-keyword ...]
          :where
          [?e :db/ident ?enum-keyword]]
        (d/db conn))
       flatten
       (apply hash-map)))


(defn datascript-db-file [conn]
  (let [datomic-datoms (all-datams-seq conn)
        enums (enum-map conn)]
    {:schema {:ingredient/name #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
              :ingredient/category #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
              :meal-plan/id #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
              :ingredient/id #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
              :recipe/id #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
              :recipe/link #:db{:cardinality :db.cardinality/one},
              :cooked-with/ingredient #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
              :cooked-with/amount-desc #:db{:cardinality :db.cardinality/one},
              :cooked-with/id #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
              :recipe/image #:db{:cardinality :db.cardinality/one},
              :cooked-with/unit #:db{:cardinality :db.cardinality/one},
              :cooked-with/recipe #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
              :cooked-with/amount #:db{:cardinality :db.cardinality/one},
              :meal-plan/recipe #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
              :meal-plan/inst #:db{:cardinality :db.cardinality/one},
              :shopping-list/meals #:db{:cardinality :db.cardinality/many, :valueType :db.type/ref},
              :recipe/type #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
              :recipe/name #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
              :meal-plan/type #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref}}
     :datoms
     (->> datomic-datoms
          (mapv
           (fn [[e a v tx r]]
             [(inc (id-munging e))
              (attr datomic-datoms a)
              (or (enums v) v)
              tx
              r])))}))

(comment
  (spit
   "resources/public/datascript-export.edn"
   (str "#datascript/DB " (datascript-db-file conn))))
