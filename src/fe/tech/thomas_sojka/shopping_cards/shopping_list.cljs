(ns tech.thomas-sojka.shopping-cards.shopping-list
  (:require [clojure.string :as str]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-sub]]))

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

(defn- attach-ingredients [db meal]
  (assoc meal :recipe (first
                       (get (->> (:main/recipes db) (group-by :id))
                            (:id (:recipe meal))))))

(reg-event-fx :shopping-list/possible-ingredients
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   (def meals-without-shopping-list meals-without-shopping-list)
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]
    :db
    (let [data (ingredients
                (->> meals-without-shopping-list
                     (map (partial attach-ingredients db))
                     (mapcat (comp :ingredients :recipe))))]
      (-> db
          (assoc :shopping-card/ingredients (vec data))
          (assoc :shopping-card/selected-ingredient-ids (set (map first data)))))}))

(defn- shopping-card-ingredients [db]
  (let [{:keys [shopping-card/ingredients shopping-card/selected-ingredient-ids]}
        db]
    (->> ingredients
         (filter #(contains? selected-ingredient-ids (first %)))
         (map second))))

(reg-event-fx :shopping-card/create
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   (js/console.log
    meals-without-shopping-list
    (shopping-card-ingredients db))
   {:db db}
   #_{:db (assoc db :app/loading true)
    :http-xhrio {:method :post
                 :uri "/shopping-card"
                 :params {:ingredients (shopping-card-ingredients db)
                          :meals meals-without-shopping-list}
                 :format (ajax/json-request-format)
                 :response-format (ajax/text-response-format)
                 :on-success [:shopping-card/success-shopping-card meals-without-shopping-list]
                 :on-failure [:shopping-card/failure-shopping-card]}}))

(reg-sub :shopping-card/ingredients
  (fn [db]
    (:shopping-card/ingredients db)))

(reg-sub :shopping-card/selected-ingredient-ids
  (fn [db]
    (:shopping-card/selected-ingredient-ids db)))

(defn toggle-map [key map]
  ((if (map key) disj conj) map key))

(reg-event-db
  :shopping-card/toggle-selected-ingredients
  (fn [db [_ id]]
    (update
     db
     :shopping-card/selected-ingredient-ids
     (partial toggle-map id))))
