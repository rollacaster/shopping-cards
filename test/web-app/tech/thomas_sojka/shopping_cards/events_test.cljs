(ns tech.thomas-sojka.shopping-cards.events-test
  (:require
   [cljs.test :as t :include-macros true]
   [tech.thomas-sojka.shopping-cards.events :as sut]))

(t/deftest main
  (t/testing "meal-plans-loaded?"
    (t/is
     (sut/meal-plans-loaded-for-month?
      {:main/meal-plans
       [{:date #inst "2022-01-09T23:00:00.000-00:00",
         :type :meal-type/dinner,
         :recipe
         {:id "6b69cebd-b4e6-424f-a908-dc325c8c2829",
          :name "Brokkoli-Nudelauflauf",
          :image
          "https://img.chefkoch-cdn.de/rezepte/90081035040865/bilder/948554/crop-360x240/brokkoli-nudelauflauf.jpg",
          :link
          "https://www.chefkoch.de/rezepte/1149701221232030/Brokkoli-Nudelauflauf.html",
          :type "NORMAL"},
         :in-shopping-list false}
        {:date #inst "2022-01-22T23:00:00.000-00:00",
         :type :meal-type/lunch,
         :recipe
         {:id "aa4c16e1-a97c-4bd1-8c97-e2438961fba1",
          :name "Breze + Tofu",
          :image
          "http://annalee-eats.de/wp-content/uploads/2017/04/P1010782-768x1024.jpg",
          :type "FAST"},
         :in-shopping-list false}]}
      0 ;; January
      ))))

(t/deftest shopping-card
  (t/testing "shopping-card-ingredients"
   (t/is
    (= (sut/shopping-card-ingredients
        {:shopping-card/ingredients [["175c3da0-a4c2-4bc4-ab37-2cfef0012ca2" "1 Kokosmilch"]
                                     ["e24ed0cd-c1a4-4e7c-92a1-84bcda090efa" "1 Zucchini"]
                                     ["960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9" "1 Karotte"]
                                     ["6c0740a2-24a0-4aa7-9548-60c79bac6fec" "1 Paprika"]
                                     ["cfef02e2-13e8-40a1-8ea3-ac55b755e0e0" "1 Ingwer"]
                                     ["4373e821-8697-48ee-becf-9776f7a4e794" "2 Knoblauch"]
                                     ["ed57f4f9-d03b-49ce-8350-d9140918de2b" "3 Erdnussbutter"]
                                     ["9e0c19af-f27f-4b04-99fd-689357ee1be8" "1 Reis"]]
         :shopping-card/selected-ingredient-ids #{"cfef02e2-13e8-40a1-8ea3-ac55b755e0e0"
                                                  "ed57f4f9-d03b-49ce-8350-d9140918de2b"
                                                  "6175d1a2-0af7-43fb-8a53-212af7b72c9c"
                                                  "175c3da0-a4c2-4bc4-ab37-2cfef0012ca2"
                                                  "4373e821-8697-48ee-becf-9776f7a4e794"
                                                  "9e0c19af-f27f-4b04-99fd-689357ee1be8"}})
       ["1 Kokosmilch" "1 Ingwer" "2 Knoblauch" "3 Erdnussbutter" "1 Reis"]))))

