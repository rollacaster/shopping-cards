(ns tech.thomas-sojka.shopping-cards.handler
  #:clj-kondo{:config
              '{:linters
                {:unresolved-symbol
                 {:exclude
                  [(compojure.api.sweet/GET)
                   (compojure.api.sweet/POST)
                   (compojure.api.sweet/PUT)
                   (compojure.api.sweet/DELETE)]}}}}
  (:require
   [clojure.instant :refer [read-instant-date]]
   [compojure.api.sweet :refer [api GET POST PUT]]
   [datomic.client.api :as d]
   [muuntaja.middleware :refer [wrap-format]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.util.response :as util.response]
   [tech.thomas-sojka.shopping-cards.db :as db]
   [tech.thomas-sojka.shopping-cards.db-sync :as db-sync]
   [tech.thomas-sojka.shopping-cards.scrape :as scrape]))


(defn app-routes [trello-client conn]
  (api
   (GET "/ws" request (#'db-sync/channel conn request))
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
   (POST "/recipe-add" request
         (let [{:keys [link image]} (:body-params request)]
           (db/transact
            conn
            (scrape/scrape-recipe conn
                                  {:link link
                                   :image image
                                   :type :recipe-type/new}))))
   (PUT "/transact" request
     (db/transact conn (mapv (fn [c] (cond-> c (:cooked-with/amount c) (update :cooked-with/amount float))) (:body-params request)))
     {:status 200})
   (POST "/query" request
     (let [{:keys [q params]} (:body-params request)]
       {:status 200 :body (d/q q (d/db conn) params)}))))

(defn app [{:keys [trello-client conn]}]
  (-> (app-routes trello-client conn)
      wrap-format
      wrap-params
      (wrap-resource "public")))
