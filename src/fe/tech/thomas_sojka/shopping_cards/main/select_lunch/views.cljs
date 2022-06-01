(ns tech.thomas-sojka.shopping-cards.main.select-lunch.views
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.main.components :as c]))

(defn select-lunch []
  (let [recipes @(subscribe [:main/lunch-recipes])]
    [c/select-recipe {:recipes recipes
                      :get-title (fn [recipe-type]
                                   [c/recipe-type-title recipe-type])}]))
