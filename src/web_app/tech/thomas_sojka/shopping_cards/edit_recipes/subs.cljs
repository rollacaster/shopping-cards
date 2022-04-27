(ns tech.thomas-sojka.shopping-cards.edit-recipes.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 :edit-recipe/recipe-details
 (fn [db [_ id]]
   (get-in db [:edit-recipe/recipes id])))

(reg-sub
 :edit-recipe/recipe-types
 :<- [:main/recipes]
 (fn [recipes]
   (set (map :type recipes))))

(reg-sub
 :edit-recipe/ingredients
 (fn [db [_ id]]
   (get-in db [:edit-recipe/ingredients id])))

(reg-sub
 :edit-recipe/all-ingredients
 (fn [db _]
   (get-in db [:edit-recipe/ingredients :all])))
