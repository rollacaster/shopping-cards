(ns tech.thomas-sojka.shopping-cards.ingredients.event
  (:require
   [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :ingredients/new
  (fn []
    {:app/push-state [:route/new-ingredient]}))

(reg-event-db :ingredients/success
  (fn [db [_ ingredients]]
    (assoc db :main/ingredients (map #(-> %
                                          first
                                          (update :ingredient/category :db/ident))
                                     ingredients))))
