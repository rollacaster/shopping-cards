(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.subs
  (:require [re-frame.core :refer [reg-sub] :as rf]))

(reg-sub
 :shopping-card/ingredients
 (fn [db _]
   (:shopping-card/ingredients db)))

(reg-sub
 :shopping-card/selected-ingredient-ids
 (fn [db _]
   (:shopping-card/selected-ingredient-ids db)))

