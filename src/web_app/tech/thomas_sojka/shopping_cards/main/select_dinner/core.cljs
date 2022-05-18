(ns tech.thomas-sojka.shopping-cards.main.select-dinner.core
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.main.components :as c]
            [tech.thomas-sojka.shopping-cards.view :as core]))

(defn select-dinner []
  (let [recipes @(subscribe [:main/sorted-recipes])]
    [c/select-recipe {:recipes recipes
                      :get-title (fn [recipe-type]
                                   [c/recipe-type-title recipe-type])}]))

(defmethod core/content :view/select-dinner [] [select-dinner])
(defmethod core/title :view/select-dinner [] "Abendessen auswählen")
