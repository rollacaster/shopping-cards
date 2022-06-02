(ns tech.thomas-sojka.shopping-cards.recipes.events
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :recipes/load
  (fn [_ [_ recipe-id]]
    {:dispatch [:query {:q '[:find (pull ?r
                                         [[:recipe/id]
                                          [:recipe/name]
                                          [:recipe/image]
                                          [:recipe/link]
                                          {[:recipe/type] [[:db/ident]]}
                                          {[:cooked-with/_recipe]
                                           [[:cooked-with/id]
                                            [:cooked-with/amount]
                                            [:cooked-with/unit]
                                            [:cooked-with/amount-desc]
                                            {[:cooked-with/ingredient]
                                             [[:ingredient/name]
                                              [:ingredient/id]]}]}])
                             :in $ ?recipe-id
                             :where [?r :recipe/id ?recipe-id]]
                        :params recipe-id
                        :on-success [:recipes/success-load]
                        :on-failure [:recipes/failure-load recipe-id]}]}))

(reg-event-fx :recipes/show-recipe
  (fn [_ [_ id]]
    {:app/push-state [:route/edit-recipe {:recipe-id id}]}))

(reg-event-fx :recipes/success-update
  (fn [{:keys [db]} [_ recipe-id]]
    {:db (-> db
             (assoc :app/loading false)
             (update :recipes dissoc recipe-id))
     :app/push-state [:route/edit-recipes]}))

(reg-event-fx :recipes/failure-update
  (fn [{:keys [db]} _]
    {:db (assoc db
                :app/loading false
                :app/error "Fehler: Update fehlgeschlagen.")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-db :recipes/success-load
  (fn [db [_ [[recipe]]]]
    (-> db
        (assoc :app/loading false)
        (assoc-in [:recipes (:recipe/id recipe)] recipe))))

(reg-event-db :recipes/failure-load
  (fn [db [_ id]]
    (-> db
        (assoc :app/loading false)
        (assoc-in [:recipes id] :ERROR))))

(reg-event-fx :recipes/new
  (fn []
    {:app/push-state [:route/new-recipe]}))
