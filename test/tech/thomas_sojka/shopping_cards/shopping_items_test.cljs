(ns tech.thomas-sojka.shopping-cards.shopping-items-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [tech.thomas-sojka.shopping-cards.shopping-items :as sut]))

(def items
  [{:shopping-item/ingredient-id "3dc8331b-e1bf-4b59-9883-96bb855f9dfd",
    :shopping-item/id "2d618d3f-ae1c-4b81-8d2d-adfe67b0eac0",
    :shopping-item/status :open,
    :shopping-item/created-at #inst "2024-01-20T12:35:00.340-00:00",
    :shopping-item/content "1 Dose Kichererbsen"}
   {:shopping-item/content "Kokosraspeln",
    :shopping-item/id "bffa7419-515d-4db1-b414-ed25bb156f95",
    :shopping-item/created-at #inst "2024-01-18T18:16:37.185-00:00",
    :shopping-item/status :open,
    :shopping-item/ingredient-id "c181aef6-0e09-43c1-85d1-2aaabdb1ce6a"}
   {:shopping-item/created-at #inst "2024-01-20T12:35:00.341-00:00",
    :shopping-item/content "5 Aufback-Brezeln",
    :shopping-item/status :open,
    :shopping-item/id "d727f974-8bad-4b00-8e7a-c568181105da",
    :shopping-item/ingredient-id "d33ad997-4d02-4437-8d35-db8e22fdb4b0"}])

(deftest shopping-items
  (testing "sorts the shopping items"
    (is
     (= (sut/sort items)
        [{:shopping-item/created-at #inst "2024-01-20T12:35:00.341-00:00",
          :shopping-item/content "5 Aufback-Brezeln",
          :shopping-item/status :open,
          :shopping-item/id "d727f974-8bad-4b00-8e7a-c568181105da",
          :shopping-item/ingredient-id "d33ad997-4d02-4437-8d35-db8e22fdb4b0"}
         {:shopping-item/content "Kokosraspeln",
          :shopping-item/id "bffa7419-515d-4db1-b414-ed25bb156f95",
          :shopping-item/created-at #inst "2024-01-18T18:16:37.185-00:00",
          :shopping-item/status :open,
          :shopping-item/ingredient-id "c181aef6-0e09-43c1-85d1-2aaabdb1ce6a"}
         {:shopping-item/ingredient-id "3dc8331b-e1bf-4b59-9883-96bb855f9dfd",
          :shopping-item/id "2d618d3f-ae1c-4b81-8d2d-adfe67b0eac0",
          :shopping-item/status :open,
          :shopping-item/created-at #inst "2024-01-20T12:35:00.340-00:00",
          :shopping-item/content "1 Dose Kichererbsen"}]))))
