(ns tech.thomas-sojka.shopping-cards.fixtures)

(def ingredients
  [#:ingredient{:id "dc1b7bdc-9f9e-4751-b935-468919d39030",
                :name "Carrot",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "bbaba432-0330-4043-90c6-3d3df2fac57b",
                :name "Onion",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f",
                :name "Mushroom",
                :category :ingredient-category/gemüse}])

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
                          :cooked-with/amount 1.0}]}])
