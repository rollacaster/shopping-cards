(ns tech.thomas-sojka.shopping-cards.data
  (:require [hodur-datomic-schema.core :as hodur-datomic]
            [hodur-engine.core :as hodur]))

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


(def datomic-schema (hodur-datomic/schema meta-db))

(comment
  )
