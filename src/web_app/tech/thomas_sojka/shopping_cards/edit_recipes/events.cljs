(ns tech.thomas-sojka.shopping-cards.edit-recipes.events
  (:require
   [ajax.core :as ajax]
   [cljs.reader :refer [read-string]]
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx
 :edit-recipe/show-recipe
 (fn [_ [_ id]]
   {:app/push-state [:route/edit-recipe {:recipe-id id}]
    :dispatch [:edit-recipe/load-ingredients id]}))

(reg-event-fx
 :edit-recipe/show-add-ingredient
 (fn [_ [_ id]]
   {:app/push-state [:route/edit-recipe-add-ingredient {:recipe-id id}]
    :dispatch [:edit-recipe/load-all-ingredients]}))

(reg-event-fx
 :edit-recipe/add-ingredient
 (fn [{:keys [db]} [_ recipe-id ingredient-id]]
   {:http-xhrio {:method :put
                 :uri (str "/recipes/" recipe-id "/ingredients/new")
                 :params {:ingredient-id ingredient-id}
                 :format (ajax/json-request-format)
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-add-ingredient recipe-id]
                 :on-failure [:edit-recipe/failure-add-ingredient recipe-id]}
    :db (assoc db :app/loading true)}))

(reg-event-fx
 :edit-recipe/success-add-ingredient
 (fn [{:keys [db]} [_ recipe-id ingredients]]
   {:db (-> db
            (assoc-in [:edit-recipe/ingredients recipe-id] ingredients)
            (assoc :app/loading false))
    :app/push-state [:route/edit-recipe {:recipe-id recipe-id}]}))

(reg-event-fx
 :edit-recipe/failure-add-ingredient
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/loading false
           :app/error "Fehler: Zutat nicht hinzugefügt.")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :edit-recipe/load-ingredients
 (fn [_ [_ id]]
   {:http-xhrio {:method :get
                 :uri (str "/recipes/" id "/ingredients")
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-load-ingredients-for-recipe id]
                 :on-failure [:edit-recipe/failure-load-ingredients-for-recipe id]}}))

(reg-event-db
 :edit-recipe/success-load-ingredients-for-recipe
 (fn [db [_ id data]]
   (assoc-in db [:edit-recipe/ingredients id] (read-string data))))

(reg-event-db
 :edit-recipe/failure-load-ingredients-for-recipe
 (fn [db [_ id]]
   (assoc-in db [:edit-recipe/ingredients id] :ERROR)))

(reg-event-fx
 :edit-recipe/load-all-ingredients
 (fn [_ _]
   {:http-xhrio {:method :get
                 :uri "/ingredients"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:edit-recipe/success-load-all-ingredients]
                 :on-failure [:edit-recipe/failure-load-all-ingredients]}}))

(reg-event-db
 :edit-recipe/success-load-all-ingredients
 (fn [db [_ ingredients]]
   (assoc-in db [:edit-recipe/ingredients :all] ingredients)))
(reg-event-db
 :edit-recipe/failure-load-all-ingredients
 (fn [db _]
   (assoc-in db [:edit-recipe/ingredients :all] :ERROR)))

(reg-event-fx
 :edit-recipe/increase-ingredient-count
 (fn [{:keys [db]} [_ recipe-id ingredient-id]]
   {:http-xhrio {:method :post
                 :uri (str "/recipes/" recipe-id "/ingredients/" ingredient-id "/inc")
                 :format (ajax/json-request-format)
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-increase-ingredient-count recipe-id]
                 :on-failure [:edit-recipe/failure-increase-ingredient-count recipe-id]}
    :db (assoc db :app/loading true)}))

(reg-event-fx
 :edit-recipe/success-increase-ingredient-count
 (fn [{:keys [db]} [_ recipe-id ingredients]]
   {:db (-> db
            (assoc-in [:edit-recipe/ingredients recipe-id] ingredients)
            (assoc :app/loading false))
    :app/push-state [:route/edit-recipe {:recipe-id recipe-id}]}))

(reg-event-fx
 :edit-recipe/failure-increase-ingredient-count
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/loading false
           :app/error "Fehler")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :edit-recipe/decrease-ingredient-count
 (fn [{:keys [db]} [_ recipe-id ingredient-id]]
   {:http-xhrio {:method :post
                 :uri (str "/recipes/" recipe-id "/ingredients/" ingredient-id "/dec")
                 :format (ajax/json-request-format)
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-decrease-ingredient-count recipe-id]
                 :on-failure [:edit-recipe/failure-decrease-ingredient-count recipe-id]}
    :db (assoc db :app/loading true)}))

(reg-event-fx
 :edit-recipe/success-decrease-ingredient-count
 (fn [{:keys [db]} [_ recipe-id ingredients]]
   {:db (-> db
            (assoc-in [:edit-recipe/ingredients recipe-id] ingredients)
            (assoc :app/loading false))
    :app/push-state [:route/edit-recipe {:recipe-id recipe-id}]}))

(reg-event-fx
 :edit-recipe/failure-decrease-ingredient-count
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/loading false
           :app/error "Fehler")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :edit-recipe/remove-ingredient
 (fn [{:keys [db]} [_ recipe-id ingredient-id]]
   {:http-xhrio {:method :delete
                 :uri (str "/recipes/" recipe-id "/ingredients/" ingredient-id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-remove-ingredient recipe-id]
                 :on-failure [:edit-recipe/failure-remove-ingredient recipe-id]}
    :db (assoc db :app/loading true)}))

(reg-event-fx
 :edit-recipe/success-remove-ingredient
 (fn [{:keys [db]} [_ recipe-id ingredients]]
   {:db (-> db
            (assoc-in [:edit-recipe/ingredients recipe-id] ingredients)
            (assoc :app/loading false))
    :app/push-state [:route/edit-recipe {:recipe-id recipe-id}]}))

(reg-event-fx
 :edit-recipe/failure-remove-ingredient
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/loading false
           :app/error "Fehler: Zutat nicht entfernt.")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :edit-recipe/edit-type
 (fn [{:keys [db]} [_ recipe-id type]]
   {:http-xhrio {:method :put
                 :uri (str "/recipes/" recipe-id)
                 :params {:type type}
                 :format (ajax/json-request-format)
                 :response-format (ajax/raw-response-format)
                 :on-success [:edit-recipe/success-edit-type recipe-id]
                 :on-failure [:edit-recipe/failure-edit-type recipe-id]}
    :db (assoc db :app/loading true)}))

(reg-event-fx
 :edit-recipe/success-edit-type
 (fn [{:keys [db]} [_ recipe-id type]]
   {:db (-> db
            (update :main/recipes #(map (fn [{:keys [id] :as recipe}]
                                          (if (= id recipe-id)
                                            (assoc recipe :type type)
                                            recipe))
                                        %))
            (assoc :app/loading false))}))

(reg-event-fx
 :edit-recipe/failure-edit-type
 (fn [{:keys [db]} _]
   {:db
    (assoc db
           :app/loading false
           :app/error "Fehler: Type nicht upgedated")
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))