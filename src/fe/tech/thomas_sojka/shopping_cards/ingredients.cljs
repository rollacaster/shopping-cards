(ns tech.thomas-sojka.shopping-cards.ingredients
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

(reg-sub :ingredients
  (fn [db]
    (:ingredients db)))

(reg-event-fx :ingredients/load
  (fn []
    {:firestore/snapshot {:path "ingredients"
                          :on-success [:ingredients/load-success]
                          :on-failure [:ingredients/load-failure]}}))

(defn ->ingredient [firestore-ingredient]
  (-> firestore-ingredient
      (set/rename-keys {:id :ingredient/id
                        :name :ingredient/name
                        :category :ingredient/category})
      (update :ingredient/category (fn [c] (keyword "ingredient-category" c)))))

(reg-event-fx :ingredients/load-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db :ingredients (map ->ingredient data))}))

(reg-event-db :ingredients/load-failure
 (fn [db _]
   (assoc db :ingredients :ERROR)))
