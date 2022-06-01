(ns tech.thomas-sojka.shopping-cards.main.deselect-ingredients.events-test
  (:require
   [cljs.test :as t :include-macros true]
   [tech.thomas-sojka.shopping-cards.main.deselect-ingredients.events :as sut]))

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

