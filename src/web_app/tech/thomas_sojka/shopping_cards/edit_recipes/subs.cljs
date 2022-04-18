(ns tech.thomas-sojka.shopping-cards.edit-recipes.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 :edit-recipe/recipe-details
 (fn [db [_ id]]
   (some #(when (= (:id %) id) %) (:main/recipes db))))

(reg-sub
 :edit-recipe/ingredients
 (fn [db [_ id]]
   (get-in db [:edit-recipe/ingredients id])))
