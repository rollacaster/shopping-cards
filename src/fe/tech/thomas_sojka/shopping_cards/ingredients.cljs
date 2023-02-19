(ns tech.thomas-sojka.shopping-cards.ingredients
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

(reg-event-fx :ingredients/new
  (fn []
    {:app/push-state [:route/new-ingredient]}))

(reg-sub :ingredients/all
  (fn [db]
    (:ingredients db)))

(reg-event-fx :ingredients/add
  (fn [_ [_ ingredient]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path "ingredients"
                           :data (assoc ingredient :id new-id)
                           :on-success [:ingredients/add-success]
                           :on-failure [:ingredients/add-failure]}})))

(reg-event-fx :ingredients/add-success
  (fn [{:keys [db]}]
    {:db (assoc db :ingredient-details/meal nil)
     :app/push-state [:route/main]}))

(reg-event-fx :ingredients/add-failure
 (fn [{:keys [db]}]
   {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

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

(reg-event-db :ingredients/load-success
 (fn [db [_ data]]
   (assoc db :ingredients (map ->ingredient data))))

(reg-event-db :main/failure-ingredients
 (fn [db _]
   (assoc db :ingredients :ERROR)))
