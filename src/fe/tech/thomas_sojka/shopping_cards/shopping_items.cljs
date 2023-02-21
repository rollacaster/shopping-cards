(ns tech.thomas-sojka.shopping-cards.shopping-items
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

(def firestore-path "shopping-items")

(reg-event-fx :shopping-item/deselect-ingredients
 (fn []
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]}))

(reg-event-fx :shopping-item/create
 (fn [_ [_ {:keys [items selected-items]}]]
   {:firestore/add-docs {:path firestore-path
                         :id :shopping-item/id
                         :data (->> items
                                    (filter (fn [[ingredient-id]] (selected-items ingredient-id)))
                                    (map (fn [[ingredient-id text]]
                                           {:shopping-item/ingredient-id ingredient-id
                                            :shopping-item/id (str (random-uuid))
                                            :shopping-item/content text
                                            :shopping-item/status :open
                                            :shopping-item/created-at (js/Date.)})))
                         :on-success [:shopping-item/add-success]
                         :on-failure [:shopping-item/add-failure]}}))

(reg-event-fx :shopping-item/add-success
 (fn []
   {:app/push-state [:route/shoppping-list]}))

(reg-event-fx :shopping-item/add-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-item/load
  (fn []
    {:firestore/snapshot {:path firestore-path
                          :on-success [:shopping-item/load-success]
                          :on-failure [:shopping-item/load-failure]}}))

(defn- ->shopping-entry [firestore-shopping-entry]
  (-> firestore-shopping-entry
      (update :status keyword)
      (update :created-at (fn [date] (.toDate date)))
      (set/rename-keys {:ingredient-id :shopping-item/ingredient-id
                        :status :shopping-item/status
                        :content :shopping-item/content
                        :created-at :shopping-item/created-at
                        :id :shopping-item/id})))

(reg-event-db :shopping-item/load-success
  (fn [db [_ data]]
    (assoc db :shopping-entries (map ->shopping-entry data))))

(reg-event-fx :shopping-item/load-failure
  (fn [{:keys [db]}]
    {:db
     (assoc db
            :app/error "Fehler: Essen nicht geladen."
            :shopping-cards [])
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-item/update
  (fn [_ [_ {:shopping-item/keys [id] :as entry}]]
    {:firestore/update-doc {:path firestore-path
                            :key id
                            :data entry}}))

(def penny-order
  [:ingredient-category/obst
   :ingredient-category/gemüse
   :ingredient-category/gewürze
   :ingredient-category/tiefkühl
   :ingredient-category/brot&co
   :ingredient-category/müsli&co
   :ingredient-category/konserven
   :ingredient-category/beilage
   :ingredient-category/backen
   :ingredient-category/fleisch
   :ingredient-category/wursttheke
   :ingredient-category/milch&co
   :ingredient-category/käse&co
   :ingredient-category/süßigkeiten
   :ingredient-category/eier
   :ingredient-category/getränke])

(defn- ingredient-text [cooked-with]
  (let [no-unit?
        (->> cooked-with
             (map (fn [[{:keys [cooked-with/unit]}]] unit))
             (every? nil?))
        amount-descs
        (map (fn [[{:keys [cooked-with/amount-desc]}]] amount-desc) cooked-with)
        amounts
        (map (fn [[{:keys [cooked-with/amount]}]] amount) cooked-with)
        no-amount? (every? nil? amount-descs)
        {:keys [:cooked-with/amount-desc]} (ffirst cooked-with)
        {:keys [ingredient/name]} (second (first cooked-with))]
    (str/trim
     (cond no-amount? name
           (= (count cooked-with) 1) (str amount-desc " " name)
           (and no-unit? no-amount?) (str (float (reduce + amounts)) " " name)
           :else (str (count cooked-with)
                      " " name
                      " (" (str/join ", " amount-descs) ")")))))

(defn ingredients [recipes]
  (->> recipes
       (group-by (comp :ingredient/id second))
       (sort-by (fn [[_ [_ {:keys [ingredient/category]}]]] category)
                (fn [category1 category2]
                  (< (.indexOf penny-order category1)
                     (.indexOf penny-order category2))))
       (map (fn [[i-id ingredients]]
              [i-id (ingredient-text ingredients)]))))

(defn- attach-ingredients [recipes meal]
  (assoc meal :recipe (first
                       (get (->> recipes (group-by :id))
                            (:id (:recipe meal))))))

(reg-sub :shopping-item/possible-items
  :<- [:recipes]
  :<- [:meals-without-shopping-list]
  (fn [[recipes meals-plans]]
    (ingredients
     (->> meals-plans
          (map (partial attach-ingredients recipes))
          (mapcat (comp :ingredients :recipe))))))

(reg-sub :shopping-entries
  (fn [db]
    (:shopping-entries db)))
