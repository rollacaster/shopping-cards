(ns tech.thomas-sojka.shopping-cards.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :app/id (s/and string? #(> (count %) 0)))

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

(s/def :cooked-with/ingredient :ingredient/id)
(s/def :cooked-with/id :app/id)
(s/def :cooked-with/amount float?)
(s/def :cooked-with/amount-desc string?)
(s/def :cooked-with/unit string?)
(s/def :cooked-with/cooked-with
  (s/keys :req [:cooked-with/ingredient
                :cooked-with/id]
          :opt [:cooked-with/amount-desc
                :cooked-with/unit
                :cooked-with/amount]))

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
