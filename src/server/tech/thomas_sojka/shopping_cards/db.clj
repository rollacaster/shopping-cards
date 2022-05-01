(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.string :as str]
            [datomic.client.api :as d]
            [tick.core :as t]))


(def penny-order
  [:ingredient-category/obst
   :ingredient-category/gemüse
   :ingredient-category/gewürze
   :ingredient-category/tiefkühl
   :ingredient-category/brot&co
   :ingredient-category/müsli&co
   :ingredient-category/konserven
   :ingredient-category/beilage
   :ingredient-category/backen
   :ingredient-category/fleisch
   :ingredient-category/wursttheke
   :ingredient-category/milch&co
   :ingredient-category/käse&co
   :ingredient-category/süßigkeiten
   :ingredient-category/eier
   :ingredient-category/getränke])

(defn transform-recipe-type [recipe]
  (-> recipe
      (assoc :type (get
                    {:recipe-type/rare "RARE"
                     :recipe-type/fast "FAST"
                     :recipe-type/normal "NORMAL"
                     :recipe-type/new "NEW"
                     :recipe-type/misc "MISC"}
                    (:db/ident (:recipe/type recipe))))
      (dissoc :recipe/type)))

(defn load-recipes [conn]
  (->> (d/db conn)
       (d/q '[:find (pull ?r [[:recipe/id :as :id]
                              [:recipe/name :as :name]
                              [:recipe/image :as :image]
                              [:recipe/link :as :link]
                              {:recipe/type [[:db/ident]]}])
              :where
              [?r :recipe/id ]])
       (map (comp transform-recipe-type first))))

(defn load-ingredients [conn]
  (->> (d/db conn)
       (d/q '[:find
              (pull ?i [[:ingredient/id :as :id]
                        [:ingredient/name :as :name]
                        {[:ingredient/category :as :category] [[:db/ident]]}])
              :where
              [?i :ingredient/id]])
       (map (fn [[ingredient]] (update ingredient :category :db/ident)))
       (sort-by :category
                (fn [category1 category2]
                  (< (.indexOf penny-order category1)
                     (.indexOf penny-order category2))))))

(defn load-entity [conn lookup-ref]
  (d/pull (d/db conn) '[*] lookup-ref))

(defn ingredients-for-recipe [conn id]
  (map
   (fn [[{:keys [ingredient/id ingredient/name]}
        {:keys [cooked-with/amount-desc]}]]
     [id (str (when amount-desc (str amount-desc " ")) name)])
   (d/q '[:find
          (pull ?i
                [:ingredient/id
                 :ingredient/name])
          (pull ?c
                [:cooked-with/amount-desc])
          :in $ ?id
          :where
          [?r :recipe/id ?id]
          [?c :cooked-with/recipe ?r]
          [?c :cooked-with/ingredient ?i]
          [?i :ingredient/id ?i-id]
          [?i :ingredient/name ?name]]
        (d/db conn)
        id)))

(defn- ingredient-text [ingredients]
  (let [no-unit?
        (->> ingredients
             (map (fn [[{:keys [cooked-with/unit]}]] unit))
             (every? nil?))
        amount-descs
        (map (fn [[{:keys [cooked-with/amount-desc]}]] amount-desc) ingredients)
        amounts
        (map (fn [[{:keys [cooked-with/amount]}]] amount) ingredients)
        no-amount? (every? nil? amount-descs)
        {:keys [cooked-with/amount-desc]} (ffirst ingredients)
        {:keys [ingredient/name]} (second (first ingredients))]
    (str/trim
     (cond no-amount? name
           (= (count ingredients) 1) (str amount-desc " " name)
           (and no-unit? no-amount?) (str (float (reduce + amounts)) " " name)
           :else (str (count ingredients)
                      " " name
                      " (" (str/join ", " amount-descs) ")")))))

(defn ingredients-for-recipes [conn selected-recipe-ids]
  (->> (d/q '[:find
            (pull ?c [:cooked-with/unit
                      :cooked-with/amount-desc
                      :cooked-with/amount])
            (pull ?i [:ingredient/id
                      :ingredient/name
                      {:ingredient/category [:db/ident]}])
            :in $ [?recipe-id ...]
            :where
            [?r :recipe/id ?recipe-id]
            [?c :cooked-with/recipe ?r]
            [?c :cooked-with/ingredient ?i]
            [?i :ingredient/name ?name]
            [?i :ingredient/category ?ca]
            [?ca :db/ident ?category]
            [?i :ingredient/id ?id]]
          (d/db conn)
          selected-recipe-ids)
     (remove (fn [[_ {{:keys [db/ident]}:ingredient/category}]]
               (or
                (= ident :ingredient-category/gewürze)
                (= ident :ingredient-category/backen))))
     (group-by (fn [[_ {:keys [ingredient/id]}]] id))
     (sort-by (fn [[_ [[_ {{:keys [db/ident]} :ingredient/category}]]]]
                ident)
              (fn [category1 category2]
                (< (.indexOf penny-order category1)
                   (.indexOf penny-order category2))))
     (map (fn [[_ ingredients]]
            (let [{:keys [ingredient/id]} (second (first ingredients))]
              [id (ingredient-text ingredients)])))))

(defn transact [conn tx-data]
  (d/transact conn {:tx-data tx-data}))

(defn retract [conn lookup-ref]
  (transact conn [[:db/retractEntity lookup-ref]]))

(defn within-next-four-days? [d1 d2]
  (let [i1 (t/instant (t/date-time (str d1 "T00:00")))
        i2 (t/instant d2)]
    (and
     (t/>= i2 i1)
     (t/< i2 (t/+ i1 (t/new-duration 4 :days))))))

(defn load-meal-plans [conn date]
  (->> (d/q '[:find (pull ?m [[:meal-plan/inst :as :date]
                              {[:meal-plan/type :as :type]
                               [[:db/ident :as :ref]]}
                              {[:meal-plan/recipe :as :recipe]
                               [[:recipe/id :as :id]
                                [:recipe/name :as :name]
                                {:recipe/type [[:db/ident]]}
                                [:recipe/image :as :image]
                                [:recipe/link :as :link]]}
                              [:shopping-list/_meals :as :shopping-list]])
              :in $ ?date
              :where
              [?m :meal-plan/inst ?d]
              [(tech.thomas-sojka.shopping-cards.db/within-next-four-days? ?date ?d)]]
            (d/db conn)
            date)
       (map (fn [meal-plan]
              (-> (first meal-plan)
                  (update :recipe transform-recipe-type)
                  (update :type :ref)
                  (update :in-shopping-list boolean))))))

(defn map->nsmap
  [m n]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                     (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k) ]
                 (assoc acc new-kw v)))
             {} m))

(defn create-meal-plan [conn meal-plan]
  (transact conn [(map->nsmap meal-plan (create-ns 'meal-plan))]))

(defn delete-meal-plan [conn {:keys [date type]}]
  (retract
   conn
   (ffirst
    (d/q
     '[:find ?id
       :in $ ?date ?type
       :where
       [?id :meal-plan/inst ?date]
       [?id :meal-plan/type ?type]]
     (d/db conn)
     date
     type))))

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
