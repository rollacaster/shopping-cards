(ns tech.thomas-sojka.shopping-cards.ingredients
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

(def firestore-path "ingredients")

(reg-event-fx :ingredients/add
  (fn [_ [_ ingredient]]
    (let [ingredient-id (str (random-uuid))
          ingredient-data (assoc ingredient :id ingredient-id)]
      {:firestore/add-doc {:path firestore-path
                           :data ingredient-data
                           :on-success [:ingredients/add-success ingredient-data]
                           :on-failure [:ingredients/add-failure]}})))

(reg-event-fx :ingredients/add-success
  (fn [_ [_ {:keys [id ingredient/name]}]]
    {:app/push-state [:route/shoppping-list]
     :dispatch [:shopping-item/add {:ingredient-id id
                                    :content name}]}))

(reg-event-fx :ingredients/add-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :ingredients/load
  (fn [_ [_ today]]
    {:firestore/snapshot {:path firestore-path
                          :on-success [:ingredients/load-success today]
                          :on-failure [:ingredients/load-failure]}}))

(defn ->ingredient [firestore-ingredient]
  (update firestore-ingredient :ingredient/category keyword))

(reg-event-fx :ingredients/load-success
  (fn [{:keys [db]} [_ now data]]
    (let [ingredients (map ->ingredient data)]
      {:db (assoc db :ingredients (map ->ingredient data))
       :dispatch-n [[:recipes/load ingredients]
                    [:meals/load now ingredients]]})))

(reg-event-db :ingredients/load-failure
 (fn [db _]
   (assoc db :ingredients :ERROR)))

(reg-sub :ingredients
  (fn [db]
    (:ingredients db)))

(reg-sub :ingredients/units
  :<- [:recipes]
  (fn [recipes]
    (->> recipes
         (mapcat :recipe/cooked-with)
         (keep :cooked-with/unit)
         set)))
