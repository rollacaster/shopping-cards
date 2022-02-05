(ns tech.thomas-sojka.shopping-cards.main.events-test
  (:require
   [cljs.test :as t :include-macros true]
   [tech.thomas-sojka.shopping-cards.main.events :as sut]))

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

