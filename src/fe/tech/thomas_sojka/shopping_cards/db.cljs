(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.spec.alpha :as s]
            [datascript.core :as d]))

(defonce conn (d/create-conn {:ingredient/name
                              #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                              :meal-plan/id
                              #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                              :ingredient/id
                              #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                              :recipe/id
                              #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                              :recipe/link #:db{:cardinality :db.cardinality/one},
                              :cooked-with/ingredient
                              #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
                              :cooked-with/amount-desc #:db{:cardinality :db.cardinality/one},
                              :cooked-with/id
                              #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity},
                              :recipe/image #:db{:cardinality :db.cardinality/one},
                              :cooked-with/unit #:db{:cardinality :db.cardinality/one},
                              :cooked-with/recipe
                              #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
                              :cooked-with/amount #:db{:cardinality :db.cardinality/one},
                              :meal-plan/recipe
                              #:db{:cardinality :db.cardinality/one, :valueType :db.type/ref},
                              :meal-plan/inst #:db{:cardinality :db.cardinality/one},
                              :shopping-list/meals
                              #:db{:cardinality :db.cardinality/many, :valueType :db.type/ref},
                              :recipe/name
                              #:db{:cardinality :db.cardinality/one, :unique :db.unique/identity}
                              :cooked-with/recipe+ingredient
                              #:db{:valueType :db.type/tuple
                                   :tupleAttrs [:cooked-with/ingredient :cooked-with/recipe]
                                   :cardinality :db.cardinality/one
                                   :unique :db.unique/identity}
                              :meal-plan/inst+type
                              #:db{:valueType :db.type/tuple
                                   :tupleAttrs [:meal-plan/inst :meal-plan/type]
                                   :cardinality :db.cardinality/one
                                   :unique :db.unique/identity}}))


(s/def :app/route map?)
(s/def :app/loading boolean?)
(s/def :app/error (s/nilable string?))

(s/def :recipe/id string?)
(s/def :recipe/name string?)
(s/def :recipe/type #{:recipe-type/normal :recipe-type/new :recipe-type/misc :recipe-type/fast :recipe-type/rare})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/recipe (s/keys :req-un [:recipe/name :recipe/image]
                              :req [:recipe/type]
                              :opt-un [:recipe/link :recipe/inactive]))
(s/def :main/recipes (s/coll-of :recipe/recipe))

(s/def :meal-plan/date inst?)
(s/def :meal-plan/type #{:meal-type/dinner :meal-type/lunch})
(s/def :meal-plan/recipe :recipe/recipe)
(s/def :meal-plan/meal
  (s/keys :req-un [:meal-plan/date :meal-plan/type]
          :opt-un [:meal-plan/recipe]))
(s/def :main/meal-plans (s/coll-of :meal-plan/meal))

(s/def :main/start-of-week inst?)
(s/def :main/bank-holidays (s/coll-of map? :kind set?))

(s/def :ingredient/id string?)
(s/def :ingredient/category keyword?)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/name]
                                      :opt [:ingredient/category]))
(s/def :shopping-card/read-ingredient (s/tuple :ingredient/id :ingredient/name))
(s/def :shopping-card/ingredients (s/coll-of :shopping-card/read-ingredient))
(s/def :shopping-card/selected-ingredient-ids (s/coll-of :ingredient/id :kind set?))

(s/def :extra-ingredients/filter string?)
(s/def :extra-ingredients/ingredients (s/coll-of :ingredient/ingredient))

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
   :extra-ingredients/filter ""
   :extra-ingredients/ingredients []
   :recipe-details/ingredients []
   :recipe-details/meal nil})
