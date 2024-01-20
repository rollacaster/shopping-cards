(ns tech.thomas-sojka.shopping-cards.migrate
  (:require ["firebase/firestore" :as firestore]
            [tech.thomas-sojka.shopping-cards.firestore :refer [db]]
            [cljs-bean.core :refer [->js ->clj]]
            [clojure.spec.alpha :as s]))

(defn add-recipe [recipe]
  (firestore/setDoc (firestore/doc (firestore/collection db "recipes")
                                   (get recipe :recipe/id))
                    (->js recipe)))

(defn add-ingredient [ingredient]
  (firestore/setDoc (firestore/doc (firestore/collection db "ingredients")
                                   (aget ingredient "ingredient/id"))
                    ingredient))

(comment
  (add-recipe recipe)

  (-> (js/fetch "/recipes.json")
      (.then (fn [res] (.json res)))
      (.then (fn [recipes] (doseq [recipe recipes]
                            (add-recipe recipe))))
      (.then js/console.log)
      (.catch js/console.log))
  (-> (js/fetch "/ingredients.json")
      (.then (fn [res] (.json res)))
      (.then (fn [ingredients] (doseq [ingredient ingredients]
                                (add-ingredient ingredient))))
      )

  ;; Load recipes
  (firestore/onSnapshot
   (firestore/query (firestore/collection db "recipes"))
   (fn [snapshot]
     (let [data (volatile! [])]
       (.forEach snapshot (fn [doc] (vswap! data conj (-> doc .data (js->clj :keywordize-keys true)))))
       (def recipes @data)))
   (fn [error]
     (js/console.log error)))

  (firestore/onSnapshot
   (firestore/query (firestore/collection db "ingredients"))
   (fn [snapshot]
     (let [data (volatile! [])]
       (.forEach snapshot (fn [doc] (vswap! data conj (-> doc .data (js->clj :keywordize-keys true)))))
       (def ingredients @data)))
   (fn [error]
     (js/console.log error)))

  ;; verify recipes
  (s/valid?
   :recipe/recipes
   (map
    (fn [recipe]
      (update recipe :recipe/type keyword))
    recipes))
  (s/valid?
   :ingredient/ingredients
   (map
    (fn [ingredient]
      (update ingredient :ingredient/category keyword))
    ingredients)))
