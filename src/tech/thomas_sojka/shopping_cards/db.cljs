(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.spec.alpha :as s]
            [clojure.test.check.generators]
            [tech.thomas-sojka.shopping-cards.specs]))

(s/def :app/loading boolean?)
(s/def :app/error (s/nilable string?))
(s/def :app/start-of-week inst?)

(s/def :bank-holidays/bank-holidays (s/coll-of map? :kind set?))

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
(s/def :shopping-item/amount int?)
(s/def :shopping-item/shopping-entry (s/keys :req [:shopping-item/ingredient-id
                                                   :shopping-item/id
                                                   :shopping-item/status
                                                   :shopping-item/content
                                                   :shopping-item/created-at]
                                             :opt [:shopping-item/amount]))

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
