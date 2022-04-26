(ns tech.thomas-sojka.integration-test
  (:require
   [cheshire.core :as json :refer [parse-string]]
   [clj-http.client :as client]
   [clojure.string :as str]
   [clojure.test :refer [deftest is use-fixtures]]
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

(deftest update-type-of-recipe
  (let [test-type "RARE"
        [{:keys [id]}] (parse-string (:body (client/get (url "/recipes"))) true)]
    (client/put (str "http://localhost:3001/recipes/" id)
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

(deftest edit-ingredient
  (let [recipe (first fixtures/recipes)
        cooked-with (first fixtures/cooked-with)
        ingredient-update {:amount 100.0 :unit "g" :amount-desc "100g"}]
    (is
     (let [ingredients (client/put (url "/recipes/" (:recipe/id recipe) "/cooked-with/" (:cooked-with/id cooked-with))
                                   {:body (json/generate-string ingredient-update)
                                    :content-type :json})]
       (some (fn [[_ name]] (str/includes? name "100g ")) (read-string (:body ingredients)))))))
