(ns tech.thomas-sojka.shopping-cards.schema
  (:require [hodur-engine.core :as hodur]
            [hodur-graphviz-schema.core :as hodur-graphviz]))

(def meta-db
  (hodur/init-schema
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
     [NORMAL RARE FAST NEW MISC]

     ^{:graphviz/tag-recursive true}
     CookedWith
     [^{:type String :datomic/unique :db.unique/identity} id
      ^Float amount
      ^Ingredient ingredient
      ^String unit
      ^String amount-desc
      ^Recipe recipe]

     ^{:enum true :graphviz/tag-recursive true}
     MealType
     [DINNER LUNCH]

     ^{:graphviz/tag-recursive true}
     MealPlan
     [^{:type String :datomic/unique :db.unique/identity} id
      ^DateTime inst
      ^MealType type
      ^Recipe recipe]

     ^{:graphviz/tag-recursive true}
     ShoppingList
     [^{:type MealPlan :cardinality [1 n]}
      meals]]))

(comment
  (do
    (spit "resources/schema.dot" (hodur-graphviz/schema meta-db))
    (clojure.java.shell/sh "dot" "-Tpng" "-oresources/schema.png" "resources/schema.dot")))
