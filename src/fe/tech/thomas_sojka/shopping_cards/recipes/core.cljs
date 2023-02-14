(ns tech.thomas-sojka.shopping-cards.recipes.core
  (:require [clojure.set :as set]
            [tech.thomas-sojka.shopping-cards.recipes.events]
            [tech.thomas-sojka.shopping-cards.recipes.recipe.core]
            [tech.thomas-sojka.shopping-cards.recipes.subs]
            [tech.thomas-sojka.shopping-cards.recipes.views :as views]
            [tech.thomas-sojka.shopping-cards.view :as core]))

(defmethod core/content :view/recipes [] [views/recipes-editing])
(defmethod core/title :view/recipes [] "Rezepte bearbeiten")

(defn ->recipe [firestore-recipe]
  (-> firestore-recipe
      (set/rename-keys {:type :recipe/type})
      (update :recipe/type (fn [t] (keyword "recipe-type" t)))))
