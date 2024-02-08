(ns tech.thomas-sojka.shopping-cards.fixtures
  (:require [tech.thomas-sojka.shopping-cards.recipes :as recipes]))

(def ingredients
  [#:ingredient{:id "bbaba432-0330-4043-90c6-3d3df2fac57b",
                :name "Onion",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "dc1b7bdc-9f9e-4751-b935-468919d39030",
                :name "Carrot",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f",
                :name "Mushroom",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "6c0740a2-24a0-4aa7-9548-60c79bac6fec",
                :name "Paprika",
                :category :ingredient-category/gemüse}])

(def shopping-items
  [#:shopping-item{:ingredient-id "bbaba432-0330-4043-90c6-3d3df2fac57b",
                   :id #uuid "3251db72-593f-4ea4-91b7-ea34e7359910",
                   :status :open,
                   :created-at #inst "2024-01-20T12:35:00.340-00:00",
                   :content "Onion"}
   #:shopping-item{:ingredient-id "dc1b7bdc-9f9e-4751-b935-468919d39030",
                   :id #uuid "475c210a-f804-4f77-ba71-05ea93da73d7",
                   :status :open,
                   :created-at #inst "2024-01-20T12:35:00.340-00:00",
                   :content "Carrot"}
   #:shopping-item{:ingredient-id "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f",
                   :id #uuid "bceadd9e-9f62-479b-a4cd-377cb564a691",
                   :status :open,
                   :created-at #inst "2024-01-20T12:35:00.340-00:00",
                   :content "Mushroom"}])

(def recipes
  [{:recipe/id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
    :recipe/link
    "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
    :recipe/image
    "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
    :recipe/type :recipe-type/fast,
    :recipe/name "Soup"
    :recipe/cooked-with [{:cooked-with/ingredient "dc1b7bdc-9f9e-4751-b935-468919d39030"
                          :cooked-with/id "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9"
                          :cooked-with/amount-desc "1"
                          :cooked-with/amount 1.0}
                         {:cooked-with/ingredient "bbaba432-0330-4043-90c6-3d3df2fac57b"
                          :cooked-with/id "3541b429-879e-419b-8597-e2451f1d4acf"
                          :cooked-with/amount-desc "1"
                          :cooked-with/amount 1.0}
                         {:cooked-with/ingredient "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f"
                          :cooked-with/id "60e71736-c2b2-4108-8a17-a5894a213786"
                          :cooked-with/amount-desc "1"
                          :cooked-with/amount 1.0}
                         {:cooked-with/ingredient "6c0740a2-24a0-4aa7-9548-60c79bac6fec"
                          :cooked-with/id #uuid "10d7e058-3fab-49b8-8ab3-26864369e383"
                          :cooked-with/amount-desc "1"
                          :cooked-with/amount 1.0},]}])

(def meal {:date #inst "2024-02-06T21:13:01.236-00:00"
           :type :meal-type/dinner
           :recipe (first (map (partial recipes/->recipe ingredients) recipes))})
