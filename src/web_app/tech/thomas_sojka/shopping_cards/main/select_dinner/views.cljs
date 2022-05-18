(ns tech.thomas-sojka.shopping-cards.main.select-dinner.views
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.main.components :as c]))

(defn select-dinner []
  (let [recipes @(subscribe [:main/sorted-recipes])]
    [c/select-recipe {:recipes recipes
                      :get-title (fn [recipe-type]
                                   [c/recipe-type-title recipe-type])}]))
