(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators]))

(s/def :app/loading boolean?)
(s/def :app/error (s/nilable string?))
(s/def :app/start-of-week inst?)
(s/def :app/id (s/and string? #(> (count %) 0)))

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
    :ingredient-category/getränke
    :ingredient-category/kosmetik})

(s/def :ingredient/id :app/id)
(s/def :ingredient/category ingredient-categorys)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/name
                                            :ingredient/category]))
(s/def :ingredient/ingredients (s/coll-of :ingredient/ingredient))

(s/def :recipe/id :app/id)
(s/def :recipe/name string?)
(s/def :recipe/type #{:recipe-type/normal :recipe-type/new :recipe-type/misc :recipe-type/fast :recipe-type/rare})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/ingredients (s/coll-of :cooked-with/cooked-with))

(s/def :recipe/recipe (s/keys :req [:recipe/type
                                    :recipe/id
                                    :recipe/name
                                    :recipe/image]
                              :opt [:recipe/link :recipe/inactive
                                    :recipe/ingredients]))
(s/def :recipe/recipes (s/coll-of :recipe/recipe))

(s/def :cooked-with/ingredient :ingredient/id)
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

(s/def :meal-plan/date inst?)
(s/def :meal-plan/type #{:meal-type/dinner :meal-type/lunch})
(s/def :meal-plan/recipe :recipe/recipe)
(s/def :meal-plan/shopping-list boolean?)
(s/def :meal-plan/meal
  (s/keys :req-un [:meal-plan/date :meal-plan/type]
          :opt-un [:meal-plan/recipe :meal-plan/shopping-list]))
(s/def :meal-plan/meals (s/coll-of :meal-plan/meal))

(s/def :shopping-item/id :app/id)
(s/def :shopping-item/ingredient-id :app/id)
(s/def :shopping-item/status #{:open :done :archive})
(s/def :shopping-item/content (s/and string? #(> (count %) 0)))
(s/def :shopping-item/created-at inst?)
(s/def :shopping-item/shopping-entry (s/keys :req [:shopping-item/ingredient-id
                                                    :shopping-item/id
                                                    :shopping-item/status
                                                    :shopping-item/content
                                                    :shopping-item/created-at]))

(s/def :shopping-item/shopping-entries (s/coll-of :shopping-item/shopping-entry))

(s/def :app/db (s/keys :req [:app/error
                             :app/loading
                             :app/start-of-week]
                       :opt-un [:shopping-item/shopping-entries]
                       :req-un [:bank-holidays/bank-holidays
                                :recipe/recipes
                                :ingredient/ingredients
                                :meal-plan/meals]))

(def default-db
  {:app/error nil
   :app/loading false
   :app/start-of-week (js/Date.)
   :ingredients []
   :recipes []
   :meals []
   :bank-holidays #{}})
