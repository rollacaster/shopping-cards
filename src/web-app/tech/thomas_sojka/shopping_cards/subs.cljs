(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub] :as rf]
            [clojure.string :as str]))

(reg-sub
 :app/error
 (fn [db _]
   (:app/error db)))

(reg-sub
 :app/route
 (fn [db _]
   (:app/route db)))

(reg-sub
 :app/loading
 (fn [db _]
   (:app/loading db)))

(reg-sub
 :shopping-card/ingredients
 (fn [db _]
   (:shopping-card/ingredients db)))

(reg-sub
 :shopping-card/selected-ingredient-ids
 (fn [db _]
   (:shopping-card/selected-ingredient-ids db)))

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
                    (:id ingredient))
                   (not (str/includes? (str/lower-case (:name ingredient))
                                       (str/lower-case ingredient-filter)))))))))

(reg-sub
 :extra-ingredients/filter
 (fn [db _]
   (:extra-ingredients/filter db)))

(reg-sub
 :recipe-details/ingredients
 (fn [db _]
   (:recipe-details/ingredients db)))

(reg-sub
 :recipe-details/meal
 (fn [db _]
   (:recipe-details/meal db)))


