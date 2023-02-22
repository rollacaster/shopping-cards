(ns tech.thomas-sojka.shopping-cards.shopping-items
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [inject-cofx reg-event-fx reg-sub]]
            [tech.thomas-sojka.shopping-cards.meal-plans :as meal-plans]
            [vimsical.re-frame.cofx.inject :as inject]))

(def firestore-path "shopping-items")

(reg-event-fx :shopping-item/deselect-ingredients
 (fn []
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]}))

(reg-event-fx :shopping-item/create
  [(inject-cofx ::inject/sub [:meals-without-shopping-list])]
  (fn [{:keys [meals-without-shopping-list]} [_ {:keys [items selected-items]}]]
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
                          :on-failure [:shopping-item/add-failure]}
     :firestore/update-docs {:path meal-plans/firestore-path
                             :id :id
                             :data (map #(assoc % :shopping-list true) meals-without-shopping-list)}}))

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

(reg-event-fx :shopping-item/load-success
  (fn [{:keys [db]} [_ data]]
    (cond-> {:db (assoc db :shopping-entries (map ->shopping-entry data))}
      (empty? (:shopping-entries db)) (assoc :dispatch [:shopping-items/archive]))))

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

(defn ingredients [cooked-with+ingredient]
  (->> cooked-with+ingredient
       (remove (fn [[_ {:keys [ingredient/category]}]]
                 (#{:ingredient-category/backen :ingredient-category/gewürze}
                  category)))
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
    (def meals-plans meals-plans)
    (def recipes recipes)
    (ingredients
     (->> meals-plans
          (map (partial attach-ingredients recipes))
          (mapcat (comp :ingredients :recipe))))))

(reg-sub :shopping-entries
  (fn [db]
    (remove (fn [{:keys [shopping-item/status]}] (= status :archive))
            (:shopping-entries db))))

(reg-event-fx :shopping-items/archive
  (fn [{{:keys [shopping-entries]} :db}]
    {:firestore/update-docs {:path firestore-path
                             :id :shopping-item/id
                             :data (->> shopping-entries
                                        (filter (fn [{:keys [shopping-item/status]}] (= status :done)))
                                        (map (fn [i] (assoc i :shopping-item/status :archive))))}}))
