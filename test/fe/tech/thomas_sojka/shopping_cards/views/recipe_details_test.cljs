(ns tech.thomas-sojka.shopping-cards.views.recipe-details-test
  (:require [tech.thomas-sojka.shopping-cards.views.recipe-details :as sut]
            [cljs.test :as t :include-macros true]))

(def recipe
  {:recipe/id "ad958898-735e-438c-847a-f89504087a89",
   :recipe/type :recipe-type/rare,
   :recipe/image
   "https://img.chefkoch-cdn.de/rezepte/570881155737167/bilder/988885/crop-360x240/avocado-pesto.jpg",
   :recipe/cooked-with
   [{:ingredient/id "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
     :cooked-with/amount "1",
     :cooked-with/amount-desc "1 EL",
     :cooked-with/unit "Esslöffel",
     :cooked-with/ingredient "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
     :ingredient/name "Passierte Tomaten",
     :ingredient/category :ingredient-category/konserven}
   {:cooked-with/unit "Zehen",
     :ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
     :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",
     :cooked-with/amount-desc "2 Zehen",
     :cooked-with/amount 2,
     :ingredient/name "Knoblauch",
     :ingredient/category :ingredient-category/gemüse}],
   :recipe/link "https://www.eat-this.org/pasta-mit-avocado-gruenkohl-pesto/",
   :recipe/name "Avocado-Pesto"})

(def ingredients [{:ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
                   :ingredient/name "Knoblauch",
                   :ingredient/category :ingredient-category/gemüse}
                  {:ingredient/name "Kräuter der Provence",
                   :ingredient/id "43a6f610-b503-4116-9c0b-b540dd5a2778",
                   :ingredient/category :ingredient-category/gewürze}])

(t/deftest recipe-editing
  (t/testing "edit cooked-with unit"
    (t/is (= (sut/update-ingredient recipe "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd" {:cooked-with/unit "asdf"})
           {:recipe/id "ad958898-735e-438c-847a-f89504087a89",
            :recipe/type :recipe-type/rare,
            :recipe/image
            "https://img.chefkoch-cdn.de/rezepte/570881155737167/bilder/988885/crop-360x240/avocado-pesto.jpg",
            :recipe/cooked-with
            [{:ingredient/id "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
              :cooked-with/amount "1",
              :cooked-with/amount-desc "1 EL",
              :cooked-with/unit "asdf",
              :cooked-with/ingredient "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
              :ingredient/name "Passierte Tomaten",
              :ingredient/category :ingredient-category/konserven}
             {:cooked-with/unit "Zehen",
              :ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
              :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",
              :cooked-with/amount-desc "2 Zehen",
              :cooked-with/amount 2,
              :ingredient/name "Knoblauch",
              :ingredient/category :ingredient-category/gemüse}],
            :recipe/link "https://www.eat-this.org/pasta-mit-avocado-gruenkohl-pesto/",
            :recipe/name "Avocado-Pesto"})))
  (t/testing "edit cooked-with amount"
    (t/is (= (sut/update-ingredient recipe "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd" {:cooked-with/amount "asdf"})
           {:recipe/id "ad958898-735e-438c-847a-f89504087a89",
            :recipe/type :recipe-type/rare,
            :recipe/image
            "https://img.chefkoch-cdn.de/rezepte/570881155737167/bilder/988885/crop-360x240/avocado-pesto.jpg",
            :recipe/cooked-with
            [{:ingredient/id "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
              :cooked-with/amount "asdf",
              :cooked-with/amount-desc "1 EL",
              :cooked-with/unit "Esslöffel",
              :cooked-with/ingredient "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
              :ingredient/name "Passierte Tomaten",
              :ingredient/category :ingredient-category/konserven}
             {:cooked-with/unit "Zehen",
              :ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
              :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",
              :cooked-with/amount-desc "2 Zehen",
              :cooked-with/amount 2,
              :ingredient/name "Knoblauch",
              :ingredient/category :ingredient-category/gemüse}],
            :recipe/link "https://www.eat-this.org/pasta-mit-avocado-gruenkohl-pesto/",
            :recipe/name "Avocado-Pesto"})))
  (t/testing "edit cooked-with replace"
    (let [new-ingredient-id "43a6f610-b503-4116-9c0b-b540dd5a2778"
          new-ingredient (some
                          (fn [{:keys [ingredient/id] :as ingredient}] (when (= id new-ingredient-id) ingredient))
                          (sut/non-recipe-ingredients ingredients recipe))]
      (t/is (= (sut/update-ingredient recipe "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd" new-ingredient)
             {:recipe/id "ad958898-735e-438c-847a-f89504087a89",
              :recipe/type :recipe-type/rare,
              :recipe/image
              "https://img.chefkoch-cdn.de/rezepte/570881155737167/bilder/988885/crop-360x240/avocado-pesto.jpg",
              :recipe/cooked-with
              [{:ingredient/id "43a6f610-b503-4116-9c0b-b540dd5a2778",
                :cooked-with/amount "1",
                :cooked-with/amount-desc "1 EL",
                :cooked-with/unit "Esslöffel",
                ;; TODO update both?
                :cooked-with/ingredient "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
                :ingredient/name "Kräuter der Provence",
                :ingredient/category :ingredient-category/gewürze}
               {:cooked-with/unit "Zehen",
                :ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
                :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",
                :cooked-with/amount-desc "2 Zehen",
                :cooked-with/amount 2,
                :ingredient/name "Knoblauch",
                :ingredient/category :ingredient-category/gemüse}],
              :recipe/link "https://www.eat-this.org/pasta-mit-avocado-gruenkohl-pesto/",
              :recipe/name "Avocado-Pesto"}))))
  (t/testing "edit cooked-with add new"
    (t/is (= (sut/add-ingredient recipe {:ingredient/id "empty",
                                         :cooked-with/amount-desc "",
                                         :cooked-with/unit "",
                                         :cooked-with/amount "",
                                         :ingredient/name ""})
          {:recipe/id "ad958898-735e-438c-847a-f89504087a89",
           :recipe/type :recipe-type/rare,
           :recipe/image
           "https://img.chefkoch-cdn.de/rezepte/570881155737167/bilder/988885/crop-360x240/avocado-pesto.jpg",
           :recipe/cooked-with
           [{:ingredient/id "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
             :cooked-with/amount "1",
             :cooked-with/amount-desc "1 EL",
             :cooked-with/unit "Esslöffel",
             :cooked-with/ingredient "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
             :ingredient/name "Passierte Tomaten",
             :ingredient/category :ingredient-category/konserven}
            {:cooked-with/unit "Zehen",
             :ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
             :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",
             :cooked-with/amount-desc "2 Zehen",
             :cooked-with/amount 2,
             :ingredient/name "Knoblauch",
             :ingredient/category :ingredient-category/gemüse}
            {:ingredient/id "empty",
             :cooked-with/amount-desc "",
             :cooked-with/unit "",
             :cooked-with/amount "",
             :ingredient/name ""}],
           :recipe/link "https://www.eat-this.org/pasta-mit-avocado-gruenkohl-pesto/",
           :recipe/name "Avocado-Pesto"})))
  (t/testing "edit cooked-with remove ingredient"
    (t/is (= (sut/remove-ingredient recipe "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd")
          {:recipe/id "ad958898-735e-438c-847a-f89504087a89",
           :recipe/type :recipe-type/rare,
           :recipe/image
           "https://img.chefkoch-cdn.de/rezepte/570881155737167/bilder/988885/crop-360x240/avocado-pesto.jpg",
           :recipe/cooked-with
           [{:cooked-with/unit "Zehen",
             :ingredient/id "4373e821-8697-48ee-becf-9776f7a4e794",
             :cooked-with/ingredient "4373e821-8697-48ee-becf-9776f7a4e794",
             :cooked-with/amount-desc "2 Zehen",
             :cooked-with/amount 2,
             :ingredient/name "Knoblauch",
             :ingredient/category :ingredient-category/gemüse}],
           :recipe/link "https://www.eat-this.org/pasta-mit-avocado-gruenkohl-pesto/",
           :recipe/name "Avocado-Pesto"}))))
