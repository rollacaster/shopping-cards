(ns tech.thomas-sojka.scrape-test
  (:require  [clojure.test :as t]
             [tech.thomas-sojka.shopping-cards.scrape :as sut]))

(t/deftest dedup-ingredients
  (t/testing "works"
    (t/is (= (sut/dedup-ingredients
              [{:id "d06107aa-68b5-41f0-8256-f124e5d0f240", :name "Kurkuma", :category :ingredient-category/gewürze}]
              [{:amount 1, :unit nil, :name "frische Kurkumawurzel", :amount-desc "1Stück"}])
             [{:amount 1, :unit nil, :name "Kurkuma", :amount-desc "1Stück", :id "d06107aa-68b5-41f0-8256-f124e5d0f240"}]))))
