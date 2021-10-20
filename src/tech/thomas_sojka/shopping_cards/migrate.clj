(ns tech.thomas-sojka.shopping-cards.migrate
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [tech.thomas-sojka.shopping-cards.schema :refer [meta-db]]))

(defn load-edn [path] (read-string (slurp (io/resource path))))
(defn load-recipes [] (load-edn "recipes.edn"))
(defn load-ingredients [] (load-edn "ingredients.edn"))
(defn load-cooked-with [] (load-edn "cooked-with.edn"))

(defn keywordify [name]
  (str/lower-case (str/replace name " " "-")))

(defn recipe-name [id]
  (first (->> (load-recipes)
              (filter #(= (:id %) id))
              (map :name))))

(defn ingredient-name [id]
  (first (->> (load-ingredients)
              (filter #(= (:id %) id))
              (map :name))))

(defn migrate-to-datomic []
  (let [client (d/client {:server-type :dev-local
                          :system "dev"})]
    (d/delete-database client {:db-name "shopping-cards"})
    (d/create-database client {:db-name "shopping-cards"})
    (let [conn (d/connect client {:db-name "shopping-cards"})
          datomic-schema (hodur-datomic/schema meta-db)]
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
                             (load-recipes))})
      (d/transact conn {:tx-data
                        (map (fn [{:keys [id category name]}]
                               {:ingredient/id id
                                :ingredient/name name
                                :ingredient/category (keyword (str "ingredient-category/" (str/replace (str/lower-case category) " " "")))})
                             (load-ingredients))})

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
                             (load-cooked-with))}))))
