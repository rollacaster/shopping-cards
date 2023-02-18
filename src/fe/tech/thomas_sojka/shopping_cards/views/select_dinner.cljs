(ns tech.thomas-sojka.shopping-cards.views.select-dinner
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defn main [match]
  (let [{:keys [query]} (:parameters match)
        {:keys [type date]} query
        recipes @(subscribe [:recipes/dinner])]
    [c/select-recipe {:recipes recipes
                      :type type
                      :date date
                      :get-title (fn [recipe-type]
                                   [c/recipe-type-title recipe-type])}]))
