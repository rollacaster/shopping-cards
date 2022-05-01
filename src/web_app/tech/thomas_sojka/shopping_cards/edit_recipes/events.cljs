(ns tech.thomas-sojka.shopping-cards.edit-recipes.events
  (:require
   [ajax.core :as ajax]
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx
 :edit-recipe/show-recipe
 (fn [_ [_ id]]
   {:app/push-state [:route/edit-recipe {:recipe-id id}]}))

(reg-event-fx
 :transact
  (fn [{:keys [db]} [_ {:keys [tx-data on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :http-xhrio {:method :put
                 :uri "/transact"
                 :params tx-data
                 :format (ajax/transit-request-format)
                 :response-format (ajax/raw-response-format)
                 :on-success on-success
                 :on-failure on-failure}}))

(reg-event-fx
 :query
  (fn [{:keys [db]} [_ {:keys [q params on-success on-failure]}]]
    {:db (assoc db :app/loading true)
     :http-xhrio {:method :post
                  :uri "/query"
                  :params {:q q
                           :params params}
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success on-success
                  :on-failure on-failure}}))


(reg-event-fx
  :edit-recipe/success-update-recipe
  (fn [{:keys [db]} [_ recipe-id]]
    {:db (-> db
             (assoc :app/loading false)
             (update :edit-recipe/recipes dissoc recipe-id))
     :app/push-state [:route/edit-recipes]}))

(reg-event-fx
  :edit-recipe/failure-update-recipe
  (fn [{:keys [db]} _]
    {:db (assoc db
                :app/loading false
                :app/error "Fehler: Update fehlgeschlagen.")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-db
 :edit-recipe/success-load-ingredients-for-recipe
 (fn [db [_ [[recipe]]]]
   (-> db
       (assoc :app/loading false)
       (assoc-in [:edit-recipe/recipes (:recipe/id recipe)] recipe))))

(reg-event-db
 :edit-recipe/failure-load-ingredients-for-recipe
 (fn [db [_ id]]
   (-> db
       (assoc :app/loading false)
       (assoc-in [:edit-recipe/recipes id] :ERROR))))

(reg-event-db
 :edit-recipe/success-load-all-ingredients
 (fn [db [_ ingredients]]
   (assoc-in db [:edit-recipe/ingredients :all] ingredients)))

(reg-event-db
 :edit-recipe/failure-load-all-ingredients
 (fn [db _]
   (assoc-in db [:edit-recipe/ingredients :all] :ERROR)))
