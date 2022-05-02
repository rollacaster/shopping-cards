(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.add-ingredients.subs
  (:require [re-frame.core :refer [reg-sub] :as rf]
            [clojure.string :as str]))

(reg-sub
 :extra-ingredients/ingredients
 (fn [db _]
   (:extra-ingredients/ingredients db)))

(reg-sub
 :extra-ingredients/addable-ingredients
 :<- [:extra-ingredients/ingredients]
 :<- [:shopping-card/ingredients]
 :<- [:extra-ingredients/filter]
 (fn [[ingredients recipe-ingredients ingredient-filter] _]
   (->> ingredients
        (remove (fn [ingredient]
                  (or
                   ((set (map first recipe-ingredients))
                    (:ingredient/id ingredient))
                   (not (str/includes? (str/lower-case (:ingredient/name ingredient))
                                       (str/lower-case ingredient-filter)))))))))

(reg-sub
 :extra-ingredients/filter
 (fn [db _]
   (:extra-ingredients/filter db)))
