(ns tech.thomas-sojka.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [tech.thomas-sojka.shopping-cards.db :as db]))

(deftest ingredients-for-recipes
  (testing "works"
    (is (= (db/ingredients-for-recipes #{"d0bb942b-0165-417d-9153-6c770c036fe8"
                                          "a1dae95b-96cf-4278-8015-9ea0fed30750"})
           [["7cc3f4e2-fc7a-41d5-a2c8-65e53d9ad641" "2 Zwiebel (1 Stück, klein, 1 Stück, klein)"]
            ["960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9" "1 Stück, klein Karotte"]
            ["3599b722-c387-433f-a20a-5ef1bcc0fd34" "2 Brokkoli (200 g, 250 g)"]
            ["175c3da0-a4c2-4bc4-ab37-2cfef0012ca2" "3 EL Kokosmilch"]
            ["6c0740a2-24a0-4aa7-9548-60c79bac6fec" "1 Stück, rot Paprika"]
            ["4373e821-8697-48ee-becf-9776f7a4e794" "1 Zehe(n) Knoblauch"]
            ["c649995f-c56f-40d9-bb65-b6e57d9c1d73" "2 EL Linsen"]
            ["14a0c9c7-3630-4ea3-957f-3807cd624636" "50 g Nudeln"]]))))

(deftest ingredients-for-recipe
  (testing "works"
    (is (= (set (db/ingredients-for-recipe "d33e56e8-64e1-4b32-9eaf-90c9405caf17"))
           (set
            [["6c0740a2-24a0-4aa7-9548-60c79bac6fec" "Paprika"]
             ["94f58d5a-221b-48d4-9f9e-118d1fdce128" "1 ½ TL Salz"]
             ["e6ce2fbe-8f6b-442e-a9e3-cdb67a1c90a1" "600 g Mehl"]
             ["3654b906-0dac-4db9-bf25-e4fbb9f4439f" "4 EL Öl"]
             ["83d5b8e1-945f-4917-9cd7-f9ddb6330b89" "1 TL Zucker"]
             ["864a7140-73c9-4360-801e-cf056886b7fb" "1/2 Hefe"]
             ["64e38f58-0fa1-4dee-8f41-fbac25a77f5f" "400 g Geriebener Käse"]
             ["5316b1b4-73e3-4431-b12c-2740e13d18f3" "4 EL Joghurt"]
             ["5df38cbe-cd05-4140-ade8-dae74db385b5" "Crème fraîche"]
             ["d7d3faa8-f7c1-4cf2-ba0b-23df648b3c7c" "Feta"]
             ["c0cf3e5e-609f-474e-9fde-810285a0c31b" "Schmand"]
             ["2a647d9b-4a02-4853-bad4-ca0f9201ed8b" "1 Eier"]
             ["6175d1a2-0af7-43fb-8a53-212af7b72c9c" "250 ml Wasser"]])))))
