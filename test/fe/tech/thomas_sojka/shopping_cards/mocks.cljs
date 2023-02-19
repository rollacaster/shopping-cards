(ns tech.thomas-sojka.shopping-cards.mocks
  (:require [re-frame.core :refer [dispatch reg-fx]]
            [tech.thomas-sojka.shopping-cards.fixtures :as fixtures]))

(def firestore-inst
  #js {"toDate" (fn [] (this-as this (.-date this)))})

(defn ->firest-store-meal-plan [meal-plan]
    (let [{:keys [date type recipe]} meal-plan
          inst (js/Object.create firestore-inst)]
      (set! (.-date inst) date)
      {:type (name type)
       :recipe recipe
       :date inst}))

(defn overwrite-firestore []
  (reg-fx :firestore/snapshot
    (fn [{:keys [path on-success]}]
      (case path
        "recipes" (dispatch (conj on-success fixtures/recipes))
        "meal-plans" (dispatch (conj on-success []))
        "ingredients" (dispatch (conj on-success fixtures/ingredients)))))

  (reg-fx :firestore/add-doc
    (fn [{:keys [path data on-success]}]
      (case path
        "meal-plans" (do
                       (dispatch [:meal/load-success [(->firest-store-meal-plan data)]])
                       (dispatch (conj on-success data)))))))
