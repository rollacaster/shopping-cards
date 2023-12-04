(ns tech.thomas-sojka.shopping-cards.mocks
  (:require [clojure.spec.alpha :as s]
            [tech.thomas-sojka.shopping-cards.recipes :as recipe]
            [re-frame.core :refer [dispatch reg-fx]]
            [tech.thomas-sojka.shopping-cards.fixtures :as fixtures]))

(def firestore-inst
  #js {"toDate" (fn [] (this-as this (.-date this)))})

(defn ->firest-store-meal-plan [meal-plan]
    (let [{:keys [date type recipe]} meal-plan
          inst (js/Object.create firestore-inst)]
      (set! (.-date inst) date)
      {:type type
       :recipe recipe
       :date inst}))

(defn ->firestore-shopping-item [shopping-item]
    (let [{:keys [shopping-item/created-at]} shopping-item
          inst (js/Object.create firestore-inst)]
      (set! (.-date inst) created-at)
      (-> shopping-item
          (assoc  :shopping-item/created-at inst))))

(defn overwrite-firestore []
  (reg-fx :firestore/snapshot
    (fn [{:keys [path on-success]}]
      (js/console.debug :firestore/snapshot path)
      (case path
        "recipes" (dispatch (conj on-success fixtures/recipes))
        "meal-plans" (dispatch (conj on-success []))
        "ingredients" (dispatch (conj on-success fixtures/ingredients))
        "shopping-items" (dispatch (conj on-success [])))))

  (reg-fx :firestore/add-doc
    (fn [{:keys [path data on-success spec key]}]
      {:pre [(s/valid? spec data)]}
      (js/console.debug :firestore/add-doc path key data)
      (case path
        "meal-plans" (do
                       (dispatch [:meal/load-success fixtures/ingredients [(->firest-store-meal-plan data)]])
                       (when on-success (dispatch (conj on-success data)))))))

  (reg-fx :firestore/add-docs
    (fn [{:keys [path data on-success spec]}]
      {:pre [(s/valid? spec data)]}
      (js/console.debug :firestore/add-docs path data)
      (case path
        "shopping-items" (dispatch [:shopping-item/load-success (map ->firestore-shopping-item data)]))
      (when on-success (dispatch (conj on-success data)))))

  (reg-fx :firestore/update-docs
    (fn [{:keys [path data on-success spec]}]
      {:pre [(s/valid? spec data)]}
      (js/console.debug :firestore/update-docs path data)
      (case path
        "meal-plans" (dispatch [:meal/load-success fixtures/ingredients (map ->firest-store-meal-plan data)])
        "shopping-items" (dispatch [:shopping-item/load-success (map ->firestore-shopping-item data)]))
      (when on-success (dispatch on-success)))))
