(ns tech.thomas-sojka.shopping-cards.datascript-export
  (:require [datascript.core]
            [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [tech.thomas-sojka.shopping-cards.schema :as schema]))

(defn domain-attrs [conn]
  (->>  (d/datoms (d/db conn) {:index :eavt :limit -1})
        (mapv (fn [[_ a]] a)) set
        (d/q
         '[:find ?v
           :in $ [?attr-id ...]
           :where [?attr-id ?a ?v]]
         (d/db conn))
        (mapcat set)
        (filter keyword?)
        (filter #(contains?
                  #{"recipe" "cooked-with" "meal-plan" "ingredient" "shopping-list"}
                  (namespace %)))
        sort))
(defn all-datams-seq [conn]
  (d/q
   '[:find ?e ?attr-name ?v ?tx ?b
     :in $ [?attr-name ...]
     :where [?e ?attr-name ?v ?tx ?b]]
   (d/db conn)
   (domain-attrs conn)))

(defn- id-munging  [id]
    (if (> id 2147483647)
      (loop [n id]
        (if (< n 2147483647)
          n
          (recur (- n 2147483647))))
      id))

(defmacro datascript-schema []
  (dissoc
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
        (apply merge))
   :meal-plan/type
   :recipe/type))

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
    {:schema (datascript-schema)
     :datoms
     (->> datomic-datoms
          (mapv
           (fn [[e a v _ _]]
             [(inc (id-munging e))
              a
              (or (enums v)
                  (cond (int? v) (inc (id-munging v))
                        (and (coll? v) (every? int? v)) (mapv #(inc (id-munging %)) v)
                        :else v))]))
          (remove (fn [[_ a]] (or (= a :cooked-with/recipe+ingredient)
                                 (= a :meal-plan/inst+type))))
          (sort-by first))}))

(comment
  (spit
   "resources/public/datascript-export.edn"
   (:datoms (datascript-db-file conn))))
