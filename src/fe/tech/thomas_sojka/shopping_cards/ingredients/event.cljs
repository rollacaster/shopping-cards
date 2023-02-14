(ns tech.thomas-sojka.shopping-cards.ingredients.event
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :ingredients/new
  (fn []
    {:app/push-state [:route/new-ingredient]}))

(reg-event-fx :ingredients/load
  (fn []
    {:firestore/snapshot {:path "ingredients"
                          :on-success [:ingredients/success]
                          :on-failure [:ingredients/failure]}}))


(defn ->ingredient [firestore-ingredient]
  (-> firestore-ingredient
      (set/rename-keys {:id :ingredient/id
                        :name :ingredient/name
                        :category :ingredient/category})
      (update :ingredient/category keyword)))

(reg-event-db :ingredients/success
  (fn [db [_ ingredients]]
    (assoc db :main/ingredients (map ->ingredient ingredients))))
