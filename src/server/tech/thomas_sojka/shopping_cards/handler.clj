(ns tech.thomas-sojka.shopping-cards.handler
  {:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [(compojure.api.sweet/GET)
                                                               (compojure.api.sweet/POST)
                                                               (compojure.api.sweet/PUT)
                                                               (compojure.api.sweet/DELETE)]}}}}
  (:require [clojure.instant :refer [read-instant-date]]
            [compojure.api.sweet :refer [api DELETE GET POST PUT]]
            [datomic.client.api :as d]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :as util.response]
            [tech.thomas-sojka.shopping-cards.db :as db]
            [tech.thomas-sojka.shopping-cards.recipe :as recipe]))

(defn app-routes [trello-client conn]
  (api
   (GET "/" [] (util.response/resource-response "index.html" {:root "public"}))
   (POST "/shopping-card" request
     {:status 201
      :body (let [{:keys [create-klaka-shopping-card]} trello-client
                  {:keys [ingredients meals]} (:body-params request)
                  trello-card-id (create-klaka-shopping-card ingredients)]
              (db/create-shopping-list
               conn
               (map
                (fn [{:keys [type date]}]
                  [(case type "lunch" :meal-type/lunch "dinner" :meal-type/dinner)
                   (read-instant-date date)])
                meals))
              trello-card-id)
      :headers {"Content-type" "application/edn"}})
   (POST "/meal-plans" request
     (let [{:keys [date recipe type]} (:body-params request)]
       (db/create-meal-plan
        conn
        {:inst (read-instant-date date)
         :type (case type "lunch" :meal-type/lunch "dinner" :meal-type/dinner)
         :recipe [:recipe/name (:name recipe)]}))
     {:status 200})
   (DELETE "/meal-plans" [date type]
     (db/delete-meal-plan
      conn
      {:date (read-instant-date date)
       :type (case type "meal-type/lunch" :meal-type/lunch "meal-type/dinner" :meal-type/dinner)})
     {:status 200})
   (GET "/recipes/:recipe-id/ingredients" [recipe-id]
     (pr-str (db/ingredients-for-recipe conn recipe-id)))
   (PUT "/recipes/:recipe-id" [recipe-id :as request]
     (recipe/edit conn recipe-id (:body-params request)))
   (GET "/ingredients" [recipe-ids]
     (if recipe-ids
       (pr-str (db/ingredients-for-recipes conn ((if (vector? recipe-ids) set hash-set) recipe-ids)))
       {:status 200
        :body (db/load-ingredients conn)
        :headers {"Content-type" "application/edn"}}))
   (PUT "/transact" request
     (db/transact conn (mapv (fn [c] (cond-> c (:cooked-with/amount c) (update :cooked-with/amount float))) (:body-params request)))
     {:status 200})
   (POST "/query" request
     (let [{:keys [q params]} (:body-params request)]
       {:status 200 :body (apply (partial d/q q (d/db conn)) params)}))))

(defn app [{:keys [trello-client conn]}]
  (-> (app-routes trello-client conn)
      wrap-format
      wrap-params
      (wrap-resource "public")))
