(ns tech.thomas-sojka.shopping-cards.data
  (:require [clojure.string :as str]
            [hodur-datomic-schema.core :as hodur-datomic]
            [hodur-engine.core :as hodur]
            [hodur-graphviz-schema.core :as hodur-graphviz]))

(defn remove-space [s]
  (str/replace s " " ""))
(set (map (comp remove-space :category) (read-string (slurp "resources/ingredients.edn"))))

(def meta-db (hodur/init-schema
              '[^{:datomic/tag true}
                default

                ^{:graphviz/tag-recursive true}
                Ingredient
                [^String name
                 ^IngredientCategory category]

                ^{:enum true :graphviz/tag-recursive true}
                IngredientCategory
                [Brot&Co Getränke Beilage Gemüse Eier Milch&Co Süßigkeiten
                 Gewürze Backen Wursttheke Fleisch Tiefkühl Müsli&Co Obst
                 Konserven Käse&Co]

                ^{:graphviz/tag-recursive true}
                Recipe
                [^String name
                 ^RecipeType type
                 ^String link
                 ^String image]

                ^{:enum true :graphviz/tag-recursive true}
                RecipeType
                [NORMAL RARE FAST]

                ^{:graphviz/tag-recursive true}
                CookedWith
                [^Float amount
                 ^Ingredient ingredient
                 ^String unit
                 ^String amount-desc
                 ^Recipe recipe]]))

(def meta-db-2 (hodur/init-schema
              '[^{:datomic/tag true}
                default

                ^{:graphviz/tag-recursive true}
                Ingredient
                [^String name
                 ^IngredientCategory category
                 ^{:type IngredientAmount
                   :cardinality [0 n]
                   :datomic/isComponent	true}
                 amount]

                ^{:graphviz/tag-recursive true}
                IngredientAmount
                [^{:type Float}
                 amount
                 ^{:type String}
                 unit
                 ^{:type String}
                 amount-desc]

                ^{:enum true :graphviz/tag-recursive true}
                IngredientCategory
                [Brot&Co Getränke Beilage Gemüse Eier Milch&Co Süßigkeiten
                 Gewürze Backen Wursttheke Fleisch Tiefkühl Müsli&Co Obst
                 Konserven Käse&Co]

                ^{:graphviz/tag-recursive true}
                Recipe
                [^String name
                 ^RecipeType type
                 ^String link
                 ^String image
                 ^{:type Ingredient
                  :cardinality [0 n]}
                 ingredients]

                ^{:enum true :graphviz/tag-recursive true}
                RecipeType
                [NORMAL RARE FAST]]))

(def graphviz-schema (hodur-graphviz/schema meta-db))
(spit "schema.dot" graphviz-schema)



(spit
 "schema.json"
 (prn-str (hodur-datomic/schema meta-db)))
(def datomic-schema (hodur-datomic/schema meta-db))

(comment
  (def meta-db-2
    (hodur/init-schema
     '[^{:datomic/tag true}
       default

       Employee
       [^String   name
        ^Float    salary
        ^Employee supervisor
        ^Integer  height [^Unit unit]]

       ^{:enum true}
       Unit
       [CENTIMETERS FEET]]))
  (hodur-datomic/schema meta-db-2))
