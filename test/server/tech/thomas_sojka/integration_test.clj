(ns tech.thomas-sojka.integration-test
  (:require
   [cheshire.core :as json :refer [parse-string]]
   [clj-http.client :as client]
   [clojure.test :refer [deftest is use-fixtures]]
   [tech.thomas-sojka.fixtures :refer [url] :as fixtures]))

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

(deftest add-ingredient-to-recipe
  (let [recipe (first fixtures/recipes)
        ingredient (first fixtures/ingredients)]
    (client/put (url "/recipes/" (:recipe/id recipe) "/ingredients/new")
                {:body (json/generate-string {:ingredient-id (:ingredient/id ingredient)})
                 :content-type :json})
    (let [ingredients (read-string (:body (client/get (url "/recipes/" (:recipe/id recipe) "/ingredients"))))]
      (is
       (some
        (fn [[id name]]
          (and (= id (:ingredient/id ingredient))
               (= name (str "1 " (:ingredient/name ingredient)))))
        ingredients)))))
