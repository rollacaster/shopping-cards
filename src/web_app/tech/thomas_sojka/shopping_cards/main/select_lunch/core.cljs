(ns tech.thomas-sojka.shopping-cards.main.select-lunch.core
  (:require [re-frame.core :refer [subscribe]]
            [tech.thomas-sojka.shopping-cards.main.components :as c]
            [tech.thomas-sojka.shopping-cards.view :as core]))

(defn select-lunch []
  (let [recipes @(subscribe [:main/lunch-recipes])]
    [c/select-recipe {:recipes recipes
                    :get-title (fn [recipe-type]
                                 [c/recipe-type-title recipe-type])}]))

(defmethod core/content :view/select-lunch [] [select-lunch])
(defmethod core/title :view/select-lunch [] "Mittag auswählen")
