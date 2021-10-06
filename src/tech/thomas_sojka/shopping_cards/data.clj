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

(defn load-recipes []
  (->> db
         (d/q '[:find ?name
                :where
                [_ :recipe/name ?name]])
         (map (fn [res]
                (d/pull
                 db
                 '[[:recipe/name :as :name]
                   [:recipe/image :as :image]
                   [:recipe/link :as :link]
                   {:recipe/type [[:db/ident]]}]
                 (vector :recipe/name (first res)))))
         (map #(-> %
                   (assoc :type (get
                                 {:recipe-type/rare "RARE"
                                  :recipe-type/fast "FAST"
                                  :recipe-type/normal "NORMAL"}
                                 (:db/ident (:recipe/type %))))
                   (dissoc :recipe/type)))))

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
