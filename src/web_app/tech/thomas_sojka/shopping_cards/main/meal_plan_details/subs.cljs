(ns tech.thomas-sojka.shopping-cards.main.meal-plan-details.subs
  (:require [re-frame.core :refer [reg-sub] :as rf]))

(reg-sub
 :recipe-details/meal
 (fn [db _]
   (:recipe-details/meal db)))
