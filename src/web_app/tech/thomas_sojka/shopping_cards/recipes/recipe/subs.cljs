(ns tech.thomas-sojka.shopping-cards.recipes.recipe.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :recipes/details
 (fn [db [_ id]]
   (get-in db [:recipes id])))
