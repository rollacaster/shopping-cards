(ns tech.thomas-sojka.core-test
  (:require
   [clj-http.client :as client]
   [clojure.test :refer [deftest is use-fixtures]]
   [clojure.walk :as walk]
   [tech.thomas-sojka.test-utils :as utils :refer [url]]))

(use-fixtures :each utils/db-setup)

(defn query-recipes []
  (-> (client/post (url "/query")
                   {:form-params {:q '[:find (pull ?r [[:recipe/id :as :id]
                                                       [:recipe/name :as :name]
                                                       [:recipe/image :as :image]
                                                       [:recipe/link :as :link]
                                                       {:recipe/type [[:db/ident]]}])
                                       :where
                                       [?r :recipe/id ]]}
                    :content-type :transit+json
                    :as :transit+json})
      :body))

(deftest load-recipes
  (let [[[recipe]] (query-recipes)]
    (is
     (=
      (dissoc recipe "id")
      {"name" "Soup",
       "image"
       "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
       "link"
       "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
       "recipe/type" {"db/ident" "recipe-type/fast"}}))))

(deftest get-recipe
  (is (= (-> (client/post (url "/query")
                          {:form-params {:q '[:find (pull ?r
                                                          [[:recipe/id]
                                                           [:recipe/name]
                                                           [:recipe/image]
                                                           [:recipe/link]
                                                           {[:recipe/type] [[:db/ident]]}
                                                           {[:cooked-with/_recipe]
                                                            [[:cooked-with/id]
                                                             [:cooked-with/amount]
                                                             [:cooked-with/unit]
                                                             [:cooked-with/amount-desc]
                                                             {[:cooked-with/ingredient]
                                                              [[:ingredient/name]
                                                               [:ingredient/id]]}]}])
                                              :in $ ?recipe-id
                                              :where [?r :recipe/id ?recipe-id]]
                                         :params "2aa44c10-bf40-476b-b95f-3bbe96a3835f"}
                           :content-type :transit+json
                           :as :transit+json})
             :body)
         [[{"recipe/id" "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
            "recipe/name" "Soup",
            "recipe/image"
            "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
            "recipe/link"
            "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
            "recipe/type" {"db/ident" "recipe-type/fast"},
            "cooked-with/_recipe"
            [{"cooked-with/id" "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
              "cooked-with/amount" 1.0,
              "cooked-with/amount-desc" "1",
              "cooked-with/ingredient"
              {"ingredient/name" "Carrot",
               "ingredient/id" "dc1b7bdc-9f9e-4751-b935-468919d39030"}}
             {"cooked-with/id" "3541b429-879e-419b-8597-e2451f1d4acf",
              "cooked-with/amount" 1.0,
              "cooked-with/amount-desc" "1",
              "cooked-with/ingredient"
              {"ingredient/name" "Onion",
               "ingredient/id" "bbaba432-0330-4043-90c6-3d3df2fac57b"}}
             {"cooked-with/id" "60e71736-c2b2-4108-8a17-a5894a213786",
              "cooked-with/amount" 1.0,
              "cooked-with/amount-desc" "1",
              "cooked-with/ingredient"
              {"ingredient/name" "Mushroom",
               "ingredient/id" "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f"}}]}]])))

(def remove-ids (partial walk/postwalk (fn [a] (cond-> a (get a "db/id") (dissoc a "db/id")))))

(deftest query
  (let [res (-> (client/post (url "/query")
                             {:form-params {:q '[:find (pull ?r [*])
                                                 :where [?r :recipe/id]]}
                              :content-type :transit+json
                              :as :transit+json})
            :body
            remove-ids)]
    (is (=
         [[{"recipe/id" "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
            "recipe/image"
            "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
            "recipe/link"
            "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
            "recipe/name" "Soup",
            "recipe/type" {"db/ident" "recipe-type/fast"}}]]
         res))))
