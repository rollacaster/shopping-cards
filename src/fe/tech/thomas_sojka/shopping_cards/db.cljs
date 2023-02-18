(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators]))

(s/def :app/route map?)
(s/def :app/loading boolean?)
(s/def :app/error (s/nilable string?))

(s/def :app/id (s/and string? #(> (count %) 0)))
(s/def :recipe/id :app/id)
(s/def :recipe/name string?)
(s/def :recipe/type #{:recipe-type/normal :recipe-type/new :recipe-type/misc :recipe-type/fast :recipe-type/rare})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/ingredients (s/coll-of (s/tuple :cooked-with/cooked-with :ingredient/ingredient)))

(s/def :recipe/recipe (s/keys :req-un [:recipe/name :recipe/image]
                              :req [:recipe/type ]
                              :opt-un [:recipe/link :recipe/inactive
                                       :recipe/ingredients]))
(s/def :main/recipes (s/coll-of :recipe/recipe))

(s/def :meal-plan/date inst?)
(s/def :meal-plan/type #{:meal-type/dinner :meal-type/lunch})
(s/def :meal-plan/recipe :recipe/recipe)
(s/def :meal-plan/shopping-list boolean?)
(s/def :meal-plan/meal
  (s/keys :req-un [:meal-plan/date :meal-plan/type]
          :opt-un [:meal-plan/recipe :meal-plan/shopping-list]))
(s/def :main/meal-plans (s/coll-of :meal-plan/meal))


(s/def :main/start-of-week inst?)
(s/def :main/bank-holidays (s/coll-of map? :kind set?))

(def ingredient-categorys
  #{:ingredient-category/obst
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
    :ingredient-category/getränke})

(s/def :ingredient/id :app/id)
(s/def :ingredient/category ingredient-categorys)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/name
                                            :ingredient/category]))

(s/def :cooked-with/ingredient :ingredient/ingredient)
(s/def :cooked-with/id :app/id)
(s/def :cooked-with/amount float?)
(s/def :cooked-with/amount-desc string?)
(s/def :cooked-with/unit string?)
(s/def :cooked-with/cooked-with
  (s/keys :opt [:cooked-with/id
                :cooked-with/amount-desc
                :cooked-with/ingredient
                :cooked-with/unit
                :cooked-with/amount]))

(s/def :shopping-card/read-ingredient (s/tuple :ingredient/id :ingredient/name))
(s/def :shopping-card/ingredients (s/coll-of :shopping-card/read-ingredient))
(s/def :shopping-card/selected-ingredient-ids (s/coll-of :ingredient/id :kind set?))

(s/def :recipe-details/ingredients (s/coll-of :shopping-card/read-ingredient))
(s/def :recipe-details/meal (s/nilable :meal-plan/meal))

(s/def :app/db (s/keys :req [:app/error
                             :app/loading
                             :app/route
                             :shopping-card/selected-ingredient-ids
                             :shopping-card/ingredients
                             :extra-ingredients/filter
                             :recipe-details/ingredients
                             :recipe-details/meal
                             :main/recipes
                             :main/meal-plans
                             :main/start-of-week]))

(def default-db
  {:app/error nil
   :app/loading false
   :app/route {}
   :main/recipes []
   :main/ingredients []
   :main/meal-plans []
   :main/start-of-week (js/Date.)
   :main/bank-holidays #{}
   :shopping-card/selected-ingredient-ids #{}
   :shopping-card/ingredients []
   :recipe-details/ingredients []
   :recipe-details/meal nil})
