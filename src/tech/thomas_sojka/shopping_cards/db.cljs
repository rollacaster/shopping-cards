(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.spec.alpha :as s]))

(s/def ::route map?)
(s/def :recipe/id string?)
(s/def :recipe/name string?)
(s/def :recipe/type #{"NORMAL" "FAST" "RARE"})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/recipe (s/keys :req-un [:recipe/id :recipe/name :recipe/type :recipe/image]
                              :opt-un [:recipe/link :recipe/inactive]))
(s/def ::recipes (s/coll-of :recipe/recipe))
(s/def ::selected-recipes (s/coll-of :recipe/id :kind set?))


(s/def :ingredient/id string?)
(s/def :ingredient/category string?)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/category :ingredient/name]))
(s/def ::read-ingredient (s/tuple :ingredient/id :ingredient/name))
(s/def ::ingredients (s/coll-of ::read-ingredient))
(s/def ::selected-ingredients (s/coll-of :ingredient/id :kind set?))
(s/def ::recipe-details (s/coll-of ::read-ingredient))

(s/def :cooked-with/recipe-id string?)
(s/def :cooked-with/ingredient-id string?)
(s/def :cooked-with/unit string?)
(s/def :cooked-with/amount-desc string?)
(s/def :cooked-with/amount number?)
(s/def :cooked-with/cooked-with (s/keys :req [:cooked-with/recipe-id :cooked-with/ingredient-id
                                              :cooked-with/unit :cooked-with/amount-desc]
                                        :opt [:cooked-with/amount]))

(s/def ::loading boolean?)

(s/def ::db (s/keys :req-un [::loading ::route ::recipes ::selected-recipes ::ingredients ::selected-ingredients ::recipe-details]))


(def default-db
  {:loading false
   :route {}
   :recipes []
   :selected-recipes #{}
   :selected-ingredients #{}
   :ingredients []
   :recipe-details []})