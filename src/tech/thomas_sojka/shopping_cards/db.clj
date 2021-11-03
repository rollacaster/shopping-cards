(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.string :as str]
            [datomic.client.api :as d]))


(def client (d/client {:server-type :dev-local
                       :system "dev"}))
(def conn (d/connect client {:db-name "shopping-cards"}))

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
                     :recipe-type/normal "NORMAL"}
                    (:db/ident (:recipe/type recipe))))
      (dissoc :recipe/type)))

(defn load-recipes []
  (->> (d/db conn)
       (d/q '[:find (pull ?r [[:recipe/id :as :id]
                              [:recipe/name :as :name]
                              [:recipe/image :as :image]
                              [:recipe/link :as :link]
                              {:recipe/type [[:db/ident]]}])
              :where
              [?r :recipe/id ]])
       (map (comp transform-recipe-type first))))

(defn load-recipe [recipe-ref]
  (d/pull
   (d/db conn)
   [[:recipe/id :as :id]
    [:recipe/name :as :name]
    [:recipe/image :as :image]
    [:recipe/link :as :link]
    {[:recipe/type :as :type] [[:db/ident :as :type]]}
    {[:cooked-with/_recipe :as :ingredients]
     [[:cooked-with/id :as :id]
      [:cooked-with/amount :as :amount]
      [:cooked-with/unit :as :unit]
      [:cooked-with/amount-desc :as :amount-desc]
      {[:cooked-with/ingredient :as :ingredient]
       [[:ingredient/name :as :name]
        [:ingredient/id :as :id]]}]}]
   recipe-ref))

(defn load-cooked-with []
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
                                  :ingredient-id ingredient-id})))))

(defn load-ingredients []
  (->> (d/db conn)
       (d/q '[:find
              (pull ?i [[:ingredient/id :as :id]
                        [:ingredient/name :as :name]
                        {[:ingredient/category :as :category] [[:db/ident]]}])
              :where
              [?i :ingredient/id ]])
       (map (fn [[ingredient]] (update ingredient :category :db/ident)))))

(defn load-entity [lookup-ref]
  (d/pull (d/db conn) '[*] lookup-ref))

(defn ingredients-for-recipe [id]
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

(defn ingredients-for-recipes [selected-recipe-ids]
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

(defn transact [tx-data]
  (d/transact conn {:tx-data tx-data}))

(defn retract [lookup-ref]
  (transact [[:db/retractEntity lookup-ref]]))

(defn month [date]
  (.format (java.text.SimpleDateFormat. "MM") date))

(defn load-meal-plans [month]
  (->> (d/q '[:find (pull ?m [[:meal-plan/inst :as :date]
                              {[:meal-plan/type :as :type]
                               [[:db/ident :as :ref]]}
                              {[:meal-plan/recipe :as :recipe]
                               [[:recipe/name :as :name]
                                {:recipe/type [[:db/ident]]}
                                [:recipe/image :as :image]]}])
              :in $ ?month
              :where
              [?m :meal-plan/inst ?d]
              [(tech.thomas-sojka.shopping-cards.db/month ?d) ?month]]
            (d/db conn)
            (str month))
       (map (fn [meal-plan]
              (-> (first meal-plan)
                  (update :recipe transform-recipe-type)
                  (update :type :ref))))))

(comment
  (load-meal-plans 10)
  (transact [#:meal-plan{:inst (java.util.Date. 121 9 5)
                         :type :meal-type/dinner
                         :recipe [:recipe/name "Teigtaschen mit Spinat-Feta-Füllung"]}])
  (defn find-recipes-by-ingredient [ingredient]
    (d/q '[:find ?name
           :in $ ?ingredient
           :where
           [?r :recipe/name ?name]
           [?c :cooked-with/recipe ?r]
           [?c :cooked-with/ingredient ?i]
           [?i :ingredient/name ?ingredient]]
         (d/db conn)
         ingredient))
  (find-recipes-by-ingredient "Brokkoli"))
