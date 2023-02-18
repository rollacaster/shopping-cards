(ns tech.thomas-sojka.shopping-cards.views.select-lunch
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn main [match]
  (let [{:keys [query]} (:parameters match)
        {:keys [type date]} query
        recipes @(subscribe [:recipes/lunch])]
    [c/select-recipe {:recipes recipes
                      :type type
                      :date date
                      :get-title (fn [recipe-type]
                                   [c/recipe-type-title recipe-type])}]))
