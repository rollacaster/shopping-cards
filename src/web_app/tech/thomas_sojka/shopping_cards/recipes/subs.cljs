(ns tech.thomas-sojka.shopping-cards.recipes.subs
  (:require
   [re-frame.core :refer [reg-sub]]))

(reg-sub
 :recipes/details
 (fn [db [_ id]]
   (get-in db [:recipes id])))

(reg-sub
 :recipes/recipe-types
 :<- [:main/recipes]
 (fn [recipes]
   (set (map :recipe/type recipes))))
