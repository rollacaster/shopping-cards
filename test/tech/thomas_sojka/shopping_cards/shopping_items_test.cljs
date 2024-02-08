(ns tech.thomas-sojka.shopping-cards.shopping-items-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [tech.thomas-sojka.shopping-cards.fixtures :as fixtures]
            [tech.thomas-sojka.shopping-cards.shopping-items :as sut]))

(def sort-order ["6c0740a2-24a0-4aa7-9548-60c79bac6fec"
                  "bbaba432-0330-4043-90c6-3d3df2fac57b"
                  "dc1b7bdc-9f9e-4751-b935-468919d39030"
                  "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f"])

(deftest shopping-items
  (testing "create shopping items list"
    (is (= (sut/create-possible-shopping-items sort-order [fixtures/meal])
           [["6c0740a2-24a0-4aa7-9548-60c79bac6fec" "1 Paprika"]
            ["bbaba432-0330-4043-90c6-3d3df2fac57b" "1 Onion"]
            ["dc1b7bdc-9f9e-4751-b935-468919d39030" "1 Carrot"]
            ["e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f" "1 Mushroom"]]))))
