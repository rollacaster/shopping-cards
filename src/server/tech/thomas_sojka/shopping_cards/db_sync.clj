(ns tech.thomas-sojka.shopping-cards.db-sync
  (:require [clojure.walk :as walk]
            [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [org.httpkit.server :as kit]
            [tech.thomas-sojka.shopping-cards.schema :refer [meta-db]]))

(defn- id-munging [datoms]
  (walk/postwalk (fn [a] (cond-> a
                          (= (:db/id a) 0) (assoc :db/id 1)
                          (:db/id a) (update :db/id (fn [id]
                                                      (if (> id 2147483647)
                                                        (loop [n id]
                                                          (if (< n 2147483647)
                                                            n
                                                            (recur (- n 2147483647))))
                                                        id)))))
                 datoms))

(defn- datascript-schema []
    (->> meta-db
         hodur-datomic/schema
         (filter (fn [{:db/keys [valueType cardinality]}]
                   (and (or valueType cardinality)
                        (or (= valueType :db.type/ref)
                            (= valueType :db.type/tuple)))))
         (mapv
          (fn [{:db/keys [ident valueType cardinality]}]
            (hash-map ident #:db{:valueType valueType
                                 :cardinality cardinality})))
         (apply merge)))

(defn- bootstrap [conn]
  (->> (d/datoms (d/db conn) {:index :eavt :limit -1})
       (map :e)
       distinct
       (map #(d/pull (d/db conn) '[*] %))
       id-munging))

(def clients (atom #{}))

(defmulti sync-handler (fn [message _ _]
                         (prn :message message)
                         (first message)))
(defmethod sync-handler :db/schema [_ ch _]
  (kit/send! ch (prn-str [:db/schema (datascript-schema)])))
(defmethod sync-handler :db/bootstrap [_ ch conn]
  (kit/send! ch (prn-str [:db/bootstrap (bootstrap conn)])))

(defn on-receive [conn ch message]
  (prn message)
  (sync-handler (read-string message) ch conn))

(defn channel [conn request]
  (d/tx-range conn {:start 0 :end 100})
  (kit/as-channel request
                  {:on-open (fn [ch]
                              (prn "open")
                              (swap! clients conj ch))
                   :on-close (fn [ch]
                               (prn "close")
                               (swap! clients (fn [clients] (remove (fn [c] (= c ch)) clients))))
                   :on-receive (partial #'on-receive conn)}))
