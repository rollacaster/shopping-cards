(ns tech.thomas-sojka.shopping-cards.handler
  {:clj-kondo/config '{:linters {:unresolved-symbol {:exclude [(compojure.api.sweet/GET)
                                                               (compojure.api.sweet/POST)
                                                               (compojure.api.sweet/PUT)
                                                               (compojure.api.sweet/DELETE)]}}}}
  (:require [compojure.api.sweet :refer [api GET POST DELETE PUT]]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [tech.thomas-sojka.shopping-cards.db
             :refer
             [ingredients-for-recipe load-recipes load-ingredients ingredients-for-recipes load-meal-plans
              create-meal-plan delete-meal-plan create-shopping-list]]
            [clojure.instant :refer [read-instant-date]]
            [tech.thomas-sojka.shopping-cards.recipe-editing :as recipe-edit]))

(defn app-routes [trello-client]
  (api
   (GET "/" [] (resource-response "index.html" {:root "public"}))
   (GET "/recipes" [] {:status 200 :body (vec (filter (comp not :inactive) (load-recipes))) :headers {"Content-type" "application/edn"}})
   (GET "/recipes/:recipe-id/ingredients" [recipe-id]
     (pr-str (ingredients-for-recipe recipe-id)))
   (GET "/ingredients" [recipe-ids]
     (if recipe-ids
       (pr-str (ingredients-for-recipes ((if (vector? recipe-ids) set hash-set) recipe-ids)))
       {:status 200
        :body (load-ingredients)
        :headers {"Content-type" "application/edn"}}))
   (POST "/shopping-card" request
     {:status 201
      :body (let [{:keys [create-klaka-shopping-card]} trello-client
                  {:keys [ingredients meals]} (:body-params request)
                  trello-card-id (create-klaka-shopping-card ingredients)]
              (create-shopping-list
               (map
                (fn [{:keys [type date]}]
                  [(case type "lunch" :meal-type/lunch "dinner" :meal-type/dinner)
                   (read-instant-date date)])
                meals))
              trello-card-id)
      :headers {"Content-type" "application/edn"}})
   (GET "/meal-plans/:date" [date]
     {:status 200
      :body (load-meal-plans date)
      :headers {"Content-type" "application/edn"}})
   (POST "/meal-plans" request
     (let [{:keys [date recipe type]} (:body-params request)]
       (create-meal-plan
        {:inst (read-instant-date date)
         :type (case type "lunch" :meal-type/lunch "dinner" :meal-type/dinner)
         :recipe [:recipe/name (:name recipe)]}))
     {:status 200})
   (DELETE "/meal-plans" [date type]
     (delete-meal-plan {:date (read-instant-date date)
                        :type (case type "lunch" :meal-type/lunch "dinner" :meal-type/dinner)})
     {:status 200})
   (PUT "/recipes/:recipe-id" request
        (let [{:keys [type]} (:body-params request)
              {:keys [recipe-id]} (:params request)]
          (recipe-edit/update-recipe-type recipe-id type)
          {:status 200}))
   (PUT "/recipes/:recipe-id/ingredients/new" [recipe-id])
   (POST "/recipes/:recipe-id/ingredients/:ingredient-id/inc" [recipe-id ingredient-id])
   (POST "/recipes/:recipe-id/ingredients/:ingredient-id/dec" [recipe-id ingredient-id])
   (DELETE "/recipes/:recipe-id/ingredients/:ingredient-id" [recipe-id ingredient-id])))

(defn app [{:keys [trello-client]}]
  (-> (app-routes trello-client)
      wrap-format
      wrap-params
      (wrap-resource "public")))
