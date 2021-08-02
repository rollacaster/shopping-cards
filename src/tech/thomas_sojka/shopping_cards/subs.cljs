(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :selected-recipes
 (fn [db _]
   (:selected-recipes db)))

(reg-sub
 :recipes
 (fn [db _]
   (:recipes db)))

(def type-order ["NORMAL" "FAST" "RARE"])
(reg-sub
 :sorted-recipes
 :<- [:recipes]
 (fn [recipes _]
   (->> recipes
        (group-by :type)
        (sort-by
         (fn [[recipe-type]] (->> type-order
                                 (map-indexed #(vector %1 %2))
                                 (some
                                  (fn [[idx recipe-type-order]]
                                    (when (= recipe-type-order recipe-type) idx)))))))))

(reg-sub
 :recipe-details
 (fn [db _]
   (prn "hi")
   (:recipe-details db)))

(reg-sub
 :shown-recipe
 :<- [:recipes]
 (fn [recipes [_ recipe-id]]
   (->> recipes
        (some #(when (= (:id %) recipe-id) %)))))

(reg-sub
 :ingredients
 (fn [db _]
   (:ingredients db)))

(reg-sub
 :route
 (fn [db _]
   (:route db)))

(reg-sub
 :loading
 (fn [db _]
   (:loading db)))

(comment
  @(subscribe [:selected-recipes])
  @(subscribe [:shown-recipe "9bbdb4ef-4934-4a96-be22-881ed37c0fd5"])

  @(subscribe [:recipes])
  @(subscribe [:sorted-recipes])
  @(subscribe [:recipe-details])
  @(subscribe [:ingredients])
  @(subscribe [:route])
  @(subscribe [:loading]))

