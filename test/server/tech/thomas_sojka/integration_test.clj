(ns tech.thomas-sojka.integration-test
  (:require
   [cheshire.core :as json :refer [parse-string]]
   [clj-http.client :as client]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
   [clojure.walk :as walk]
   [tech.thomas-sojka.fixtures :as fixtures :refer [url]]))

(use-fixtures :each fixtures/db-setup)

(deftest load-recipes
  (let [[{:keys [name image link type]}] (parse-string (:body (client/get "http://localhost:3001/recipes")) true)]
    (is
     (and (= name "Misosuppe mit Gemüse und Tofu2")
          (= image "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg")
          (= link "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html")
          (= type "FAST")))))

(deftest ingredients-for-recipes
  (let [[{:keys [id]}] (parse-string (:body (client/get (url "/recipes"))) true)]
    (is
     (= (mapv second (read-string (:body (client/get (url "/ingredients?recipe-ids=" id)))))
        ["1 große Mandarine"]))))

(deftest ingredients-for-recipe
  (let [[{:keys [id]}] (parse-string (:body (client/get (url "/recipes"))) true)]
    (is
     (= (mapv second (read-string (:body (client/get (url "/recipes/" id "/ingredients")))))
        ["1 große Mandarine"]))))

(deftest get-recipe
  (is (= (-> (client/get (url "/recipes/" (:recipe/id (first fixtures/recipes))))
             :body
             read-string)
         {:recipe/id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
          :recipe/link
          "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
          :recipe/image
          "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
          :recipe/type {:db/ident :recipe-type/fast},
          :recipe/name "Misosuppe mit Gemüse und Tofu2"
          :cooked-with/_recipe [#:cooked-with{:ingredient #:ingredient{:name "Mandarine",
                                                                       :id "61858d61-a9d0-4ba6-b341-bdcdffec50d1"},
                                              :amount-desc "1 große",
                                              :id "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
                                              :amount 1.0}]})))

(deftest edit-recipe
  (let [test-type "RARE"]
    (client/put (url "/recipes/" (:recipe/id (first fixtures/recipes)))
                {:body (json/generate-string {:type test-type}) :content-type :json})
    (let [[{:keys [type]}] (parse-string (:body (client/get (url "/recipes"))) true)]
      (is (= type test-type)))))

(deftest create-cooked-with
  (let [ingredient (first fixtures/ingredients)]
    (client/post (url "/cooked-with")
                 {:body (json/generate-string
                         {:ingredient-id (:ingredient/id ingredient)
                          :recipe-id (:recipe/id (first fixtures/recipes))})
                  :content-type :json})
    (is
     (some
      (fn [[_ ingredient-name]]
        (str/includes? ingredient-name (:ingredient/name ingredient)))
      (-> (client/get (url "/recipes/" (:recipe/id (first fixtures/recipes)) "/ingredients"))
          :body
          read-string)))))

(deftest remove-cooked-with
  (client/delete (url "/cooked-with/" (:cooked-with/id (first fixtures/cooked-with))))
  (is
   (empty?
    (-> (client/get (url "/recipes/" (:recipe/id (first fixtures/recipes)) "/ingredients"))
        :body
        read-string))))

(deftest edit-cooked-with
  (client/put (url "/cooked-with/" (:cooked-with/id (first fixtures/cooked-with)))
              {:body (json/generate-string {:amount 100.0 :unit "g" :amount-desc "100g"})
               :content-type :json})
  (is
   (-> (client/get (url "/recipes/" (:recipe/id (first fixtures/recipes)) "/ingredients"))
       :body
       read-string)))

(def remove-ids (partial walk/postwalk (fn [a] (cond-> a (get a "db/id") (dissoc a "db/id")))))

(deftest query
  (let [res (-> (client/post (url "/query")
                         {:form-params '[:find (pull ?r [*])
                                         :where [?r :recipe/id]]
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
