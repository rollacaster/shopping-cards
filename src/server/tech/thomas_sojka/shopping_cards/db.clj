(ns tech.thomas-sojka.shopping-cards.db
  (:require [datomic.client.api :as d]
            [tick.core :as t]))

(defn transact [conn tx-data]
  (d/transact conn {:tx-data tx-data}))

(defn within-next-four-days? [d1 d2]
  (let [i1 (t/instant (t/date-time (str d1 "T00:00")))
        i2 (t/instant d2)]
    (and
     (t/>= i2 i1)
     (t/< i2 (t/+ i1 (t/new-duration 4 :days))))))

(defn create-shopping-list [conn meal-plans]
  (transact
   conn
   [#:shopping-list
    {:meals
     (map first
          (d/q '[:find ?id
                 :in $ [[?type ?inst]]
                 :where
                 [?id :meal-plan/type ?type]
                 [?id :meal-plan/inst ?inst]]
               (d/db conn)
               meal-plans))}]))

(comment
  (defn find-recipes-by-ingredient [conn ingredient]
    (d/q '[:find ?name
           :in $ ?ingredient
           :where
           [?r :recipe/name ?name]
           [?c :cooked-with/recipe ?r]
           [?c :cooked-with/ingredient ?i]
           [?i :ingredient/name ?ingredient]]
         (d/db conn)
         ingredient))
  (defn load-cooked-with [conn]
    (->> (d/db conn)
         (d/q '[:find
                (pull ?c
                      [[:cooked-with/amount :as :amount]
                       [:cooked-with/ingredient :as :ingredient-id]
                       [:cooked-with/unit :as :unit]
                       [:cooked-with/amount-desc :as :amount-desc]
                       [:cooked-with/id :as :id]])
                ?recipe-id
                ?ingredient-id
                :where
                [?c :cooked-with/id]
                [?c :cooked-with/recipe ?r]
                [?c :cooked-with/ingredient ?i]
                [?r :recipe/id ?recipe-id]
                [?i :ingredient/id ?ingredient-id]])
         (map (fn [[cooked-with recipe-id ingredient-id]]
                (merge cooked-with {:recipe-id recipe-id
                                    :ingredient-id ingredient-id}))))))
