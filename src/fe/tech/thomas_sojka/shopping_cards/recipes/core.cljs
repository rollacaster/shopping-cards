(ns tech.thomas-sojka.shopping-cards.recipes.core
  (:require [clojure.set :as set]
            [tech.thomas-sojka.shopping-cards.ingredients.event :as ingredients.event]
            [tech.thomas-sojka.shopping-cards.recipes.events]
            [tech.thomas-sojka.shopping-cards.recipes.recipe.core]
            [tech.thomas-sojka.shopping-cards.recipes.subs]
            [tech.thomas-sojka.shopping-cards.recipes.views :as views]
            [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/recipes [] [views/recipes-editing])
(defmethod core/title :view/recipes [] "Rezepte bearbeiten")

(defn ->recipe [firestore-recipe]
  (cond-> firestore-recipe
      :always (set/rename-keys {:type :recipe/type})
      :always(update :recipe/type (fn [t] (keyword "recipe-type" t)))
      (:ingredients firestore-recipe)
      (update :ingredients (fn [cooked-with]
                             (map
                              (fn [{:keys [ingredient] :as c}]
                                [(-> c
                                     (set/rename-keys {:unit :cooked-with/unit
                                                       :amount-desc :cooked-with/amount-desc
                                                       :amount :cooked-with/amount})
                                     (dissoc :ingredient))
                                 (ingredients.event/->ingredient ingredient)])
                              cooked-with)))))
