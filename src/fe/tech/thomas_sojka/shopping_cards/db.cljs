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
(s/def :recipe/recipes (s/coll-of :recipe/recipe))

(s/def :meal-plan/date inst?)
(s/def :meal-plan/type #{:meal-type/dinner :meal-type/lunch})
(s/def :meal-plan/recipe :recipe/recipe)
(s/def :meal-plan/shopping-list boolean?)
(s/def :meal-plan/meal
  (s/keys :req-un [:meal-plan/date :meal-plan/type]
          :opt-un [:meal-plan/recipe :meal-plan/shopping-list]))
(s/def :meals-plans/meals (s/coll-of :meal-plan/meal))



(s/def :app/start-of-week inst?)
(s/def :bank-holidays/bank-holidays (s/coll-of map? :kind set?))

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
(s/def :ingredient/ingredients (s/coll-of :ingredient/ingredient))

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

(s/def :app/db (s/keys :req [:app/error
                             :app/loading
                             :app/route
                             :app/start-of-week
                             :meals-plans/meals]
                       :req-un [:bank-holidays/bank-holidays
                                :recipe/recipes
                                :ingredient/ingredients]))

(def default-db
  {:app/error nil
   :app/loading false
   :app/route {}
   :app/start-of-week (js/Date.)
   :ingredients []
   :recipes []
   :meals-plans/meals []
   :bank-holidays #{}})

(comment
  (do
    (def db @re-frame.db/app-db)
    (tap> db)))
