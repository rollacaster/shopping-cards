(ns tech.thomas-sojka.shopping-cards.ingredients.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :ingredients/all
  (fn [db]
    (:main/ingredients db)))
