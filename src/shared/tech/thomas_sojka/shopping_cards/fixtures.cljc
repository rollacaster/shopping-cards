(ns tech.thomas-sojka.shopping-cards.fixtures)

(def ingredients
  [#:ingredient{:id "dc1b7bdc-9f9e-4751-b935-468919d39030",
                :name "Carrot",
                :category "gemüse"}
   #:ingredient{:id "bbaba432-0330-4043-90c6-3d3df2fac57b",
                :name "Onion",
                :category "gemüse"}
   #:ingredient{:id "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f",
                :name "Mushroom",
                :category "gemüse"}])

(def cooked-with [{:ingredient {:name "Carrot"
                                :category "gemüse"
                                :id "dc1b7bdc-9f9e-4751-b935-468919d39030",},
                   :id "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
                   :amount-desc "1",
                   :amount 1.0}
                  {:ingredient {:name "Onion"
                                :category "gemüse"
                                :id "bbaba432-0330-4043-90c6-3d3df2fac57b",},
                   :id "3541b429-879e-419b-8597-e2451f1d4acf",
                   :amount-desc "1",
                   :amount 1.0}
                  {:ingredient {:name "Mushroom"
                                :category "gemüse"
                                :id "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f",},
                   :id "60e71736-c2b2-4108-8a17-a5894a213786",
                   :amount-desc "1",
                   :amount 1.0}])

(def recipes
  [{:id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
    :link
    "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
    :image
    "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
    :recipe/type :recipe-type/fast,
    :name "Soup"
    :ingredients cooked-with}])
