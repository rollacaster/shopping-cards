(ns tech.thomas-sojka.shopping-cards.ingredients.event
  (:require [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx :ingredients/new
  (fn []
    {:app/push-state [:route/new-ingredient]}))
