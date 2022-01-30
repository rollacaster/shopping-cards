(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.spec.alpha :as s]))

(s/def :app/route map?)
(s/def :app/loading boolean?)
(s/def :app/error (s/nilable string?))

(s/def :recipe/id string?)
(s/def :recipe/name string?)
(s/def :recipe/type #{"NORMAL" "FAST" "RARE" "NEW" "MISC"})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/recipe (s/keys :req-un [:recipe/name :recipe/type :recipe/image]
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

(s/def :ingredient/id string?)
(s/def :ingredient/category string?)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/category :ingredient/name]))
(s/def :shopping-card/read-ingredient (s/tuple :ingredient/id :ingredient/name))
(s/def :shopping-card/ingredients (s/coll-of :shopping-card/read-ingredient))
(s/def :shopping-card/selected-ingredient-ids (s/coll-of :ingredient/id :kind set?))

(s/def :extra-ingredients/filter string?)

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
   :main/meal-plans []
   :main/start-of-week (js/Date.)
   :shopping-card/selected-ingredient-ids #{}
   :shopping-card/ingredients []
   :extra-ingredients/filter ""
   :recipe-details/ingredients []
   :recipe-details/meal nil})
