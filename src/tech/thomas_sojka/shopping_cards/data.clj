(ns tech.thomas-sojka.shopping-cards.data
  (:require [clojure.string :as str]
            [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [hodur-engine.core :as hodur]
            [tech.thomas-sojka.shopping-cards.db :as db]))

(def meta-db (hodur/init-schema
              '[^{:datomic/tag true}
                default

                ^{:graphviz/tag-recursive true}
                Ingredient
                [^{:type String :datomic/unique :db.unique/identity} id
                 ^{:type String :datomic/unique :db.unique/identity} name
                 ^IngredientCategory category]

                ^{:enum true :graphviz/tag-recursive true}
                IngredientCategory
                [Brot&co Getränke Beilage Gemüse Eier Milch&co Süßigkeiten
                 Gewürze Backen Wursttheke Fleisch Tiefkühl Müsli&co Obst
                 Konserven Käse&co]

                ^{:graphviz/tag-recursive true}
                Recipe
                [^{:type String :datomic/unique :db.unique/identity} id
                 ^{:type String :datomic/unique :db.unique/identity} name
                 ^RecipeType type
                 ^{:type String
                   :optional true} link
                 ^String image]

                ^{:enum true :graphviz/tag-recursive true}
                RecipeType
                [NORMAL RARE FAST]

                ^{:graphviz/tag-recursive true}
                CookedWith
                [^{:type String :datomic/unique :db.unique/identity} id
                 ^Float amount
                 ^Ingredient ingredient
                 ^String unit
                 ^String amount-desc
                 ^Recipe recipe]]))


(def client (d/client {:server-type :dev-local
                           :system "dev"}))
(def conn (d/connect client {:db-name "shopping-cards"}))
(def db (d/db conn))

(def penny-order
  [:ingredient-category/obst
   :ingredient-category/gemüse
   :ingredient-category/gewürze
   :ingredient-category/tiefkühl
   :ingredient-category/brot-&-Co
   :ingredient-category/müsli-&-Co
   :ingredient-category/konserven
   :ingredient-category/beilage
   :ingredient-category/backen
   :ingredient-category/fleisch
   :ingredient-category/wursttheke
   :ingredient-category/milch-&-Co
   :ingredient-category/käse-&-Co
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
  (->> db
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
  (->> db
       (d/q '[:find
              (pull ?i [[:ingredient/id :as :id]
                        [:ingredient/name :as :name]
                        {[:ingredient/category :as :category] [[:db/ident]]}])
              :where
              [?i :ingredient/id ]
              [?c :cooked-with/ingredient ?i]])
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
        db
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
            [?c :cooked-with/amount-desc ?amount-desc]
            [?c :cooked-with/amount ?amount]
            [?i :ingredient/name ?name]
            [?i :ingredient/category ?ca]
            [?ca :db/ident ?category]
            [?i :ingredient/id ?id]]
          db
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


(comment
  (defn keywordify [name]
    (str/lower-case (str/replace name " " "-")))
  (defn recipe-name [id]
    (first (->> (db/load-recipes)
                (filter #(= (:id %) id))
                (map :name))))
  (defn ingredient-name [id]
    (first (->> (db/load-ingredients)
                (filter #(= (:id %) id))
                (map :name))))


  (d/delete-database client {:db-name "shopping-cards"})
  (d/create-database client {:db-name "shopping-cards"})
  (def conn (d/connect client {:db-name "shopping-cards"}))
  (def datomic-schema (hodur-datomic/schema meta-db))
  (d/transact conn {:tx-data datomic-schema})
  (d/transact conn {:tx-data [{:db/ident :cooked-with/recipe+ingredient
                               :db/valueType :db.type/tuple
                               :db/tupleAttrs [:cooked-with/ingredient :cooked-with/recipe]
                               :db/cardinality :db.cardinality/one
                               :db/unique :db.unique/identity}]})

  (d/transact conn {:tx-data
                    (map (fn [{:keys [id name type link image]}]
                           (cond-> {:recipe/id id
                                    :recipe/name name
                                    :recipe/type (keyword (str "recipe-type/" (str/lower-case type)))
                                    :recipe/image image}
                             link
                             (assoc :recipe/link link)))
                         (db/load-recipes))})
  (d/transact conn {:tx-data
                    (map (fn [{:keys [id category name]}]
                           {:ingredient/id id
                            :ingredient/name name
                            :ingredient/category (keyword (str "ingredient-category/" (str/replace (str/lower-case category) " " "")))})
                         (db/load-ingredients))})

  (d/transact conn {:tx-data
                    (map (fn [{:keys [id ingredient-id recipe-id unit amount-desc amount]}]
                           (cond->
                               {:cooked-with/id id
                                :cooked-with/ingredient [:ingredient/name (ingredient-name ingredient-id)]
                                :cooked-with/recipe [:recipe/name (recipe-name recipe-id)]}
                             ((complement str/blank?) amount-desc)
                             (assoc :cooked-with/amount-desc amount-desc)
                             unit
                             (assoc :cooked-with/unit unit)
                             amount
                             (assoc :cooked-with/amount (if (nil? amount) nil (float amount)))))
                         (db/load-cooked-with))})

  (def db (d/db conn))
  (defn find-recipes-by-ingredient [ingredient]
    (d/q '[:find ?name
           :in $ ?ingredient
           :where
           [?r :recipe/name ?name]
           [?c :cooked-with/recipe ?r]
           [?c :cooked-with/ingredient ?i]
           [?i :ingredient/name ?ingredient]]
         db
         ingredient))
  (find-recipes-by-ingredient "Brokkoli"))
