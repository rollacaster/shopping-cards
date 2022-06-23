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
      {"name" "Misosuppe mit Gemüse und Tofu2",
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
            "recipe/name" "Misosuppe mit Gemüse und Tofu2",
            "recipe/image"
            "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
            "recipe/link"
            "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
            "recipe/type" {"db/ident" "recipe-type/fast"},
            "cooked-with/_recipe"
            [{"cooked-with/id" "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
              "cooked-with/amount" 1.0,
              "cooked-with/amount-desc" "1 große",
              "cooked-with/ingredient"
              {"ingredient/name" "Mandarine",
               "ingredient/id" "61858d61-a9d0-4ba6-b341-bdcdffec50d1"}}]}]])))

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
            "recipe/name" "Misosuppe mit Gemüse und Tofu2",
            "recipe/type" {"db/ident" "recipe-type/fast"}}]]
         res))))
