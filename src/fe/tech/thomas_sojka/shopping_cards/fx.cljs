(ns tech.thomas-sojka.shopping-cards.fx
  (:require
   [re-frame.core :refer [dispatch reg-fx reg-cofx]]
   [datascript.core :as d]
   [reagent.core :as r]
   [reitit.frontend.easy :as rfe]))

(reg-fx :app/push-state
        (fn [route]
          (apply rfe/push-state route)))

(reg-fx :app/scroll-to
        (fn [[x y]]
          (.scrollTo js/window x y)))

(defonce timeouts (r/atom {}))

(reg-fx
 :app/timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @timeouts id)]
     (js/clearTimeout existing)
     (swap! timeouts dissoc id))
   (when (some? event)
     (swap! timeouts assoc id
            (js/setTimeout
             (fn []
               (dispatch event))
             time)))))

(def conn (d/create-conn {:ingredient/name
                          #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                          :meal-plan/id
                         #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                          :ingredient/id
                          #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                          :recipe/id
                          #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                          :recipe/link #:db{:cardinality :db.cardinality/one},
                          :cooked-with/ingredient
                          #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
                          :cooked-with/amount-desc #:db{:cardinality :db.cardinality/one},
                          :cooked-with/id
                          #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                          :recipe/image #:db{:cardinality :db.cardinality/one},
                          :cooked-with/unit #:db{:cardinality :db.cardinality/one},
                          :cooked-with/recipe
                          #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
                          :cooked-with/amount #:db{:cardinality :db.cardinality/one},
                          :meal-plan/recipe
                          #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
                          :meal-plan/inst #:db{:cardinality :db.cardinality/one},
                          :shopping-list/meals
                          #:db{:cardinality :db.cardinality/many, :valueType :db.type/ref},
                          :recipe/name
                          #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity}
                          :cooked-with/recipe+ingredient
                          #:db{:valueType :db.type/tuple
                               :tupleAttrs [:cooked-with/ingredient :cooked-with/recipe]
                               :cardinality :db.cardinality/one
                               :unique :db.unique/identity}
                          :meal-plan/inst+type
                          #:db{:valueType :db.type/tuple
                               :tupleAttrs [:meal-plan/inst :meal-plan/type]
                               :cardinality :db.cardinality/one
                               :unique :db.unique/identity}}))

(reg-cofx :app/conn
  (fn [coeffects]
    (assoc coeffects :conn conn)))

(reg-fx :app/init-datoms
  (fn [datoms]
    (d/transact! conn
                 (->> datoms
                      (mapv (fn [d] (into [:db/add] d)))))))
