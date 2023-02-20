(ns tech.thomas-sojka.shopping-cards.shopping-list
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

(def firestore-path "shoppping-cards")

(reg-event-fx :shopping-list/deselect-ingredients
 (fn []
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]}))

(reg-event-fx :shopping-card/create
 (fn [_ [_ {:keys [items selected-items]}]]
   (let [new-id (str (random-uuid))]
     {:firestore/add-doc {:path firestore-path
                          :data {:entries (->> items
                                               (filter (fn [[ingredient-id]] (selected-items ingredient-id)))
                                               (map (fn [[ingredient-id text]]
                                                      {:shopping-entry/ingredient-id ingredient-id
                                                       :shopping-entry/item text
                                                       :shopping-entry/status :open})))
                                 :id new-id}
                          :on-success [:shopping-card/add-success new-id]
                          :on-failure [:shopping-card/add-failure]}})))

(reg-event-fx :shopping-card/add-success
 (fn [_ [_ card-id]]
   {:app/push-state [:route/shoppping-card {:card-id card-id}]}))

(reg-event-fx :shopping-card/add-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-cards/load
  (fn []
    {:firestore/snapshot {:path firestore-path
                          :on-success [:shopping-card/load-success]
                          :on-failure [:shopping-card/load-failure]}}))

(defn- ->shopping-entry [firestore-shopping-entry]
  (-> firestore-shopping-entry
      (update :status keyword)
      (set/rename-keys {:ingredient-id :shopping-entry/ingredient-id
                        :status :shopping-entry/status
                        :item :shopping-entry/item})))

(defn- ->shopping-card [firestore-shopping-card]
  (-> firestore-shopping-card
      (update :entries (fn [entries] (map ->shopping-entry entries)))
      (set/rename-keys {:entries :shopping-card/entries
                        :id :shopping-card/id})))

(reg-event-db :shopping-card/load-success
  (fn [db [_ data]]
    (assoc db :shopping-cards (map ->shopping-card data))))

(reg-event-fx :shopping-card/load-failure
  (fn [{:keys [db]}]
    {:db
     (assoc db
            :app/error "Fehler: Essen nicht geladen."
            :shopping-cards [])
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

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

(reg-sub :shopping-list/possible-items
  :<- [:recipes]
  :<- [:meals-without-shopping-list]
  (fn [[recipes meals-plans]]
    (ingredients
     (->> meals-plans
          (map (partial attach-ingredients recipes))
          (mapcat (comp :ingredients :recipe))))))

(reg-sub :shopping-cards
  (fn [db]
    (:shopping-cards db)))

(reg-sub :shopping-cards/active
  :<- [:shopping-cards]
  (fn [shopping-cards]
    ;; TODO filter active
    shopping-cards))

(reg-sub :shopping-list/current
  :<- [:shopping-cards/active]
  (fn [shopping-cards]
    (some
     (fn [shopping-card]
       ;; TODO check for active
       (when shopping-card shopping-card))
     shopping-cards)))

(reg-sub :shopping-list/shopping-card
  :<- [:shopping-cards]
  (fn [shopping-cards [_ card-id]]
   (some
     (fn [{:shopping-card/keys [id] :as shopping-card}]
       (when (= id card-id) shopping-card))
     shopping-cards)))
