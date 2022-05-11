(ns tech.thomas-sojka.shopping-cards.ingredient-add.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub :ingredient/categories
  (fn [db]
    (:categories db)))
