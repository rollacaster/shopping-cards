(ns tech.thomas-sojka.integration-test
  (:require
   [cheshire.core :as json :refer [parse-string]]
   [clj-http.client :as client]
   [clojure.test :refer [deftest is use-fixtures]]
   [datomic.client.api :as d]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl]
   [tech.thomas-sojka.shopping-cards.db :as db]
   [tech.thomas-sojka.shopping-cards.recipe-editing :as recipes]))

(def db-name "shopping-cards-test")
(def config
  {:adapter/jetty {:port 3001
                   :trello-client (ig/ref :external/trello-client)
                   :conn (ig/ref :datomic/dev-local)}
   :external/trello-client {}
   :datomic/dev-local {:db-name db-name}})

(defn populate []
  (let [client (d/client {:server-type :dev-local :system "dev"})
        conn (d/connect client {:db-name db-name})
        {:keys [ingredient/id ingredient/name] :as ingredient} (assoc (recipes/add-ingredient {:category :ingredient-category/obst :name "Mandarine"})
                                                                      :db/id "new-ingredient")
        recipe (recipes/add-new-recipe
                 {:name "Misosuppe mit Gemüse und Tofu2",
                  :link "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
                  :type "FAST",
                  :inactive false,
                  :image "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
                  :ingredients [{:amount-desc "1 große", :name name, :amount 1, :id id}]})]
    (db/transact conn (conj recipe ingredient))))


(defn db-setup [test-run]
  (ig-repl/set-prep! (fn [] config))
  (ig-repl/go)
  (populate)
  (test-run)
  (d/delete-database (d/client {:server-type :dev-local :system "dev"})
                     {:db-name db-name})
  (ig-repl/halt))

(use-fixtures :each db-setup)

(deftest load-recipes
  (let [[{:keys [name image link type]}] (parse-string (:body (client/get "http://localhost:3001/recipes")) true)]
    (is
     (and (= name "Misosuppe mit Gemüse und Tofu2")
          (= image "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg")
          (= link "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html")
          (= type "FAST")))))

(deftest ingredients-for-recipes
  (let [[{:keys [id]}] (parse-string (:body (client/get "http://localhost:3001/recipes")) true)]
    (is
     (= (mapv second (read-string (:body (client/get (str "http://localhost:3001/ingredients?recipe-ids=" id)))))
        ["1 große Mandarine"]))))

(deftest ingredients-for-recipe
  (let [[{:keys [id]}] (parse-string (:body (client/get "http://localhost:3001/recipes")) true)]
    (is
     (= (mapv second (read-string (:body (client/get (str "http://localhost:3001/recipes/" id "/ingredients")))))
        ["1 große Mandarine"]))))

(deftest update-type-of-recipe
  (let [test-type "RARE"
        [{:keys [id]}] (parse-string (:body (client/get "http://localhost:3001/recipes")) true)]
    (client/put (str "http://localhost:3001/recipes/" id)
                {:body (json/generate-string {:type test-type}) :content-type :json})
    (let [[{:keys [type]}] (parse-string (:body (client/get "http://localhost:3001/recipes")) true)]
      (is (= type test-type)))))
