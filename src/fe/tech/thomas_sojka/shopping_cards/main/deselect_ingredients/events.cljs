(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.events
  (:require
   [ajax.core :as ajax]
   [re-frame.core :refer [reg-event-fx reg-event-db]]
   [tech.thomas-sojka.shopping-cards.ingredients-processing :as ingredients]
   [tech.thomas-sojka.shopping-cards.queries :as queries]))

(reg-event-fx
 :shopping-card/failure-shopping-card
 (fn [{:keys [db]} _]
   {:app/push-state [:tech.thomas-sojka.shopping-cards.view/error]
    :db (assoc db :app/loading false)}))

(reg-event-fx
 :shopping-card/success-shopping-card
 (fn [{:keys [db]} [_ meals-without-shopping-list card-id]]
   {:db (-> db
            (assoc :app/loading false)
            ;; TODO update to has shopping-list
            (update :main/meal-plans #(map (fn [meal-plan]
                                             (if
                                                 (some
                                                  (fn [meal-with-shopping-list]
                                                    (and
                                                     (= (:type meal-with-shopping-list) (:type meal-plan))
                                                     (= (:date meal-with-shopping-list) (:date meal-plan))))
                                                  meals-without-shopping-list)
                                               (assoc meal-plan :shopping-list true)
                                               meal-plan))
                                           (:main/meal-plans db)))
            (assoc :extra-ingredients {}))
    :app/push-state [:route/finish {:card-id card-id}]}))

(defn toggle-map [key map]
  ((if (map key) disj conj) map key))

(reg-event-db
 :shopping-card/toggle-selected-ingredients
 (fn [db [_ id]]
   (update
    db
    :shopping-card/selected-ingredient-ids
    (partial toggle-map id))))

;; TODO is the the :shopping-card entry?
(reg-event-fx :shopping-card/load-ingredients-for-meals
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]
    :db
    (let [data (ingredients/process-ingredients
                (mapcat (comp :ingredients :recipe) meals-without-shopping-list))]
      (-> db
          (assoc :shopping-card/ingredients (vec data))
          (assoc :shopping-card/selected-ingredient-ids (set (map first data)))))}))

(defn shopping-card-ingredients [db]
  (let [{:keys [shopping-card/ingredients shopping-card/selected-ingredient-ids]}
        db]
    (->> ingredients
         (filter #(contains? selected-ingredient-ids (first %)))
         (map second))))

(reg-event-fx :shopping-card/create
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   {:db db
    :firestore/add-docs {:path "meal-plans"
                         :data (map (fn [meal] (update meal :recipe dissoc :ingredients)) meals-without-shopping-list)}}))
