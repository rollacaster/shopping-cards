(ns tech.thomas-sojka.ingredients.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [muuntaja.middleware :refer [wrap-format]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [tech.thomas-sojka.ingredients.core
             :refer
             [create-klaka-shopping-card ingredients-for-recipes load-recipes]]))

(defroutes app-routes
  (GET "/recipes" [] {:status 200 :body (vec (load-recipes)) :headers {"Content-type" "application/edn"}})
  (GET "/ingredients" [recipe-ids] (pr-str (ingredients-for-recipes (set recipe-ids))))
  (POST "/shopping-card" request
        {:status 201
         :body (create-klaka-shopping-card (:body-params request))
         :headers {"Content-type" "application/edn"}})
  (route/not-found "<h1>Page not found</h1>"))

(def app
  (-> app-routes
      (wrap-defaults
       {:params    {:urlencoded true
                    :multipart  true
                    :nested     true
                    :keywordize true}
        :cookies   true
        :session   {:flash true
                    :cookie-attrs {:http-only true, :same-site :strict}}
        :security  {:anti-forgery   false
                    :xss-protection {:enable? true, :mode :block}
                    :frame-options  :sameorigin
                    :content-type-options :nosniff}
        :static    {:resources "public"}
        :responses {:not-modified-responses true
                    :absolute-redirects     true
                    :content-types          true
                    :default-charset        "utf-8"}})
      wrap-format
      (wrap-cors  :access-control-allow-origin [#"http://localhost:9504"]
                  :access-control-allow-methods [:get :put :post :delete])))


