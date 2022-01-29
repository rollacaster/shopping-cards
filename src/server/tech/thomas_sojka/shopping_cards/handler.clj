(ns tech.thomas-sojka.shopping-cards.handler
  (:require [compojure.api.sweet :refer [api GET POST DELETE]]
            [compojure.route :as route]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [tech.thomas-sojka.shopping-cards.trello
             :refer [create-klaka-shopping-card]]
            [tech.thomas-sojka.shopping-cards.db
             :refer
             [ingredients-for-recipe load-recipes load-ingredients ingredients-for-recipes load-meal-plans
              create-meal-plan delete-meal-plan create-shopping-list]]
            [clojure.instant :refer [read-instant-date]]))

(def app-routes
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
          :body (create-klaka-shopping-card (:body-params request))
          :headers {"Content-type" "application/edn"}})
   (GET "/meal-plans/:month" [month]
     {:status 200
      :body (load-meal-plans month)
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
   (POST "/shopping-list" request
     (create-shopping-list
      (map
       (fn [[type date]]
         [(case type "lunch" :meal-type/lunch "dinner" :meal-type/dinner)
          (read-instant-date date)])
       (:body-params request)))
     {:status 200})

   (route/not-found "<h1>Page not found</h1>")))
(def app
  (-> app-routes
      wrap-format
      wrap-params
      (wrap-resource "public")))


