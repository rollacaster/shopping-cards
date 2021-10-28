(ns tech.thomas-sojka.shopping-cards.db
  (:require
   [cljs.spec.gen.alpha :as gen]
   [clojure.spec.alpha :as s]))

(s/def ::route map?)
(s/def :recipe/id string?)
(s/def :recipe/name string?)
(s/def :recipe/type #{"NORMAL" "FAST" "RARE"})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/recipe (s/keys :req-un [:recipe/name :recipe/type :recipe/image]
                              :opt-un [:recipe/link :recipe/inactive]))
(s/def ::recipes (s/coll-of :recipe/recipe))
(s/def ::selected-recipes (s/coll-of :recipe/name :kind set?))


(s/def :ingredient/id string?)
(s/def :ingredient/category string?)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/category :ingredient/name]))
(s/def ::read-ingredient (s/tuple :ingredient/id :ingredient/name))
(s/def ::ingredients (s/coll-of ::read-ingredient))
(s/def ::selected-ingredients (s/coll-of :ingredient/id :kind set?))
(s/def ::recipe-details (s/coll-of ::read-ingredient))

(s/def :cooked-with/recipe-id :recipe/id)
(s/def :cooked-with/ingredient-id string?)
(s/def :cooked-with/unit string?)
(s/def :cooked-with/amount-desc string?)
(s/def :cooked-with/amount number?)
(s/def :cooked-with/cooked-with (s/keys :req [:cooked-with/ingredient-id
                                              :cooked-with/unit :cooked-with/amount-desc]
                                        :opt [:cooked-with/amount]))

(s/def ::loading boolean?)

(s/def :meal-plan/date inst?)
(s/def :meal-plan/type #{:LUNCH :DINNER})
(s/def :meal-plan/recipe-id :recipe/id)
(s/def :meal-plan/meal-plan
  (s/keys :req-un [:meal-plan/date :meal-plan/type :meal-plan/recipe-id]))
(s/def ::meal-plans (s/coll-of :meal-plan/meal-plan))

(s/def ::db (s/keys :req-un [::loading ::route ::recipes ::selected-recipes ::ingredients ::selected-ingredients ::recipe-details
                             ::meal-plans]))


(def default-db
  {:loading false
   :route {}
   :recipes []
   :selected-recipes #{}
   :selected-ingredients #{}
   :ingredients []
   :meal-plans []
   :recipe-details []})
