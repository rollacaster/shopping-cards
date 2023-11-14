(ns fe.tech.thomas-sojka.shopping-cards.recipe-details
  (:require  [cljs.test :as t :include-macros true]
             [tech.thomas-sojka.shopping-cards.recipes :as recipes]
             [tech.thomas-sojka.shopping-cards.firestore :as firestore]))

(def recipe-id "dc20d162-3405-4e41-8f58-34ebc7626128")
(def recipes [{:recipe/id "dc20d162-3405-4e41-8f58-34ebc7626128",
               :recipe/cooked-with
               [{:cooked-with/amount 2,
                 :cooked-with/amount-desc "2",
                 :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",}
                {:cooked-with/ingredient "9580eac9-5902-4420-917d-7d6539c64c9b",}],
               :recipe/link
               "https://docs.google.com/document/d/1JfASF4sBJAFdkB-HTb83s21o3lFjasFcYIqnKtwDWFM/edit#heading=h.e96n3ds06edj",
               :recipe/type :recipe-type/rare,
               :recipe/image
               "http://flowers-images-de.s3.amazonaws.com/wp-content/uploads/avocado-basilikum-creme-pasta_flowers-on-my-plate.jpg",
               :recipe/name "Avocado-Basilikum-Pasta"}])
(def ingredients [{:ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
                   :ingredient/category :ingredient-category/gemüse,
                   :ingredient/name "Knoblauch"}
                  {:ingredient/id "9580eac9-5902-4420-917d-7d6539c64c9b",
                   :ingredient/name "Pfeffer",
                   :ingredient/category :ingredient-category/gewürze}])

(t/deftest recipe-details
  (t/testing "update with firebase"
      (t/async done
        (let [update-path [:recipe/cooked-with 0 :cooked-with/amount]
              value-to-test 4
              recipe (->> recipes
                          ;; building recipes on load
                          (map (partial recipes/->recipe
                                        (recipes/ingredient-id->ingredient ingredients)))
                          ;; finding recipes on view
                          (recipes/find-recipe recipe-id))
              data (-> recipe
                       ;; Transform in UI
                       (assoc-in update-path value-to-test)
                       ;; transforming recipes on save
                       recipes/->firestore-recipe)]
          ;; Save to firebase
          (-> (firestore/update-doc data "recipes" recipe-id)
              (.then (fn [] (firestore/get-doc "recipes" recipe-id)))
              (.then (fn [recipe] (t/is (= (get-in recipe update-path) value-to-test))))
              (.then (fn [] (firestore/delete-doc "recipes" recipe-id)))
              (.then (fn [] (firestore/get-doc "recipes" recipe-id)))
              (.then (fn [recipe] (t/is (nil? recipe))
                       (done)))
              (.catch js/console.log))))))
