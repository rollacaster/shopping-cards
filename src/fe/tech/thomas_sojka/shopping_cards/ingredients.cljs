(ns tech.thomas-sojka.shopping-cards.ingredients
  (:require [cljs.reader :as reader]
            [clojure.set :as set]
            [re-frame.core :refer [inject-cofx reg-cofx reg-event-db
                                   reg-event-fx reg-fx reg-sub]]))

(reg-sub :ingredients
  (fn [db]
    (:ingredients db)))

(def localstorage-ingredients-key "ingredients")

(reg-cofx :local-store-ingredients
  (fn [cofx]
    (assoc cofx :local-store-ingredients
           (some->> (.getItem js/localStorage localstorage-ingredients-key)
                    (reader/read-string)))))

(reg-event-fx :ingredients/load
  [(inject-cofx :local-store-ingredients)]
  (fn [{:keys [db local-store-ingredients]}]
    (if (empty? local-store-ingredients)
      {:firestore/snapshot {:path "ingredients"
                            :on-success [:ingredients/load-success]
                            :on-failure [:ingredients/load-failure]}}
      {:db (assoc db :ingredients local-store-ingredients)})))

(defn ->ingredient [firestore-ingredient]
  (-> firestore-ingredient
      (set/rename-keys {:id :ingredient/id
                        :name :ingredient/name
                        :category :ingredient/category})
      (update :ingredient/category (fn [c] (keyword "ingredient-category" c)))))

(reg-fx :ingredients/store
  (fn [ingredients]
    (.setItem js/localStorage localstorage-ingredients-key (str ingredients))))

(reg-event-fx :ingredients/load-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db :ingredients (map ->ingredient data))
     :ingredients/store (map ->ingredient data)}))

(reg-event-db :ingredients/load-failure
 (fn [db _]
   (assoc db :ingredients :ERROR)))
