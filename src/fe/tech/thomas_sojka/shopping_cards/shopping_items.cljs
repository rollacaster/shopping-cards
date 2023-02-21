(ns tech.thomas-sojka.shopping-cards.shopping-items
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))
(prn :load)
(def firestore-path "shopping-entries")

(reg-event-fx :shopping-entry/deselect-ingredients
 (fn []
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]}))

(reg-event-fx :shopping-entry/create
 (fn [_ [_ {:keys [items selected-items]}]]
   {:firestore/add-docs {:path firestore-path
                         :id :shopping-entry/id
                         :data (->> items
                                    (filter (fn [[ingredient-id]] (selected-items ingredient-id)))
                                    (map (fn [[ingredient-id text]]
                                           {:shopping-entry/ingredient-id ingredient-id
                                            :shopping-entry/id (str (random-uuid))
                                            :shopping-entry/item text
                                            :shopping-entry/status :open
                                            :shopping-entry/created-at (js/Date.)})))
                         :on-success [:shopping-entry/add-success]
                         :on-failure [:shopping-entry/add-failure]}}))

(reg-event-fx :shopping-entry/add-success
 (fn []
   {:app/push-state [:route/shoppping-card]}))

(reg-event-fx :shopping-entry/add-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-entry/load
  (fn []
    {:firestore/snapshot {:path firestore-path
                          :on-success [:shopping-entry/load-success]
                          :on-failure [:shopping-entry/load-failure]}}))

(defn- ->shopping-entry [firestore-shopping-entry]
  (-> firestore-shopping-entry
      (update :status keyword)
      (update :created-at (fn [date] (.toDate date)))
      (set/rename-keys {:ingredient-id :shopping-entry/ingredient-id
                        :status :shopping-entry/status
                        :item :shopping-entry/item
                        :created-at :shopping-entry/created-at
                        :id :shopping-entry/id})))

(reg-event-db :shopping-entry/load-success
  (fn [db [_ data]]
    (assoc db :shopping-entries (map ->shopping-entry data))))

(reg-event-fx :shopping-entry/load-failure
  (fn [{:keys [db]}]
    {:db
     (assoc db
            :app/error "Fehler: Essen nicht geladen."
            :shopping-cards [])
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-entry/update
  (fn [_ [_ {:shopping-entry/keys [id] :as entry}]]
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

(reg-sub :shopping-entry/possible-items
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
