(ns tech.thomas-sojka.ingredients.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [tech.thomas-sojka.ingredients.core
             :refer
             [create-klaka-shopping-card ingredients-for-recipes load-recipes]]))

(defroutes app-routes
  (GET "/recipes" [] {:status 200 :body (vec (load-recipes)) :headers {"Content-type" "application/edn"}})
  (GET "/ingredients" [recipe-ids]
       (pr-str (ingredients-for-recipes ((if (seq? recipe-ids) set hash-set) recipe-ids ))))
  (POST "/shopping-card" request
        {:status 201
         :body (create-klaka-shopping-card (:body-params request))
         :headers {"Content-type" "application/edn"}})
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> app-routes
      wrap-format
      wrap-params
      (wrap-cors  :access-control-allow-origin [#"*"]
                  :access-control-allow-methods [:get :put :post :delete])))


