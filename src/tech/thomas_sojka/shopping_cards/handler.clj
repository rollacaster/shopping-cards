(ns tech.thomas-sojka.shopping-cards.handler
  (:require [compojure.api.sweet :refer [api GET POST]]
            [compojure.route :as route]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [resource-response]]
            [tech.thomas-sojka.shopping-cards.core
             :refer [create-klaka-shopping-card]]
            [tech.thomas-sojka.shopping-cards.db
             :refer
             [ingredients-for-recipe load-recipes ingredients-for-recipes]]))

(def app-routes
  (api
   (GET "/" [] (resource-response "index.html" {:root "public"}))
   (GET "/recipes" [] {:status 200 :body (vec (filter (comp not :inactive) (load-recipes))) :headers {"Content-type" "application/edn"}})
   (GET "/recipes/:recipe-id/ingredients" [recipe-id]
     (pr-str (ingredients-for-recipe recipe-id)))
   (GET "/ingredients" [recipe-ids]
     (pr-str (ingredients-for-recipes ((if (vector? recipe-ids) set hash-set) recipe-ids))))
   (POST "/shopping-card" request
     {:status 201
      :body (create-klaka-shopping-card (:body-params request))
      :headers {"Content-type" "application/edn"}})
   (route/not-found "<h1>Page not found</h1>")))

(def app
  (-> app-routes
      wrap-format
      wrap-params
      (wrap-resource "public")))


