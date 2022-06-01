(ns tech.thomas-sojka.shopping-cards.schema
  (:require [hodur-engine.core :as hodur]))

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

     MealPlan
     [^{:type String :datomic/unique :db.unique/identity} id
      ^DateTime inst
      ^MealType type
      ^Recipe recipe]

     ShoppingList
     [^{:type MealPlan :cardinality [1 n]}
      meals]]))
