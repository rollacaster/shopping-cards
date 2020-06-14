(ns tech.thomas-sojka.ingredients.auth
  (:require [clj-http.client :as client]
            [clojure.core.async :refer [<!! >! chan go]]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [tick.core :as t]))

(def oauth-creds-path (str (System/getProperty "user.home") "/.google-oauth-creds"))
(def oauth-creds
  (try
    (read-string (slurp oauth-creds-path))
    (catch java.io.FileNotFoundException _ nil)))

(def creds (atom nil))

(defn create-authorization-request [params]
  (let [google-auth-url "https://accounts.google.com/o/oauth2/v2/auth"
        params (s/join "&"
                       (map
                        (fn [[key value]] (str (name key) "=" (if (coll? value) (s/join " " value) value)))
                        (assoc params :access_type "offline" :response_type "code")))]
    (str google-auth-url "?" params)))

(defn receive-token []
  (let [c (chan 1)
        server (atom nil)]
    (reset! server (run-jetty (wrap-params (fn [request]
                                             (when (get (:query-params request) "code")
                                               (go (>! c (get (:query-params request) "code"))))
                                             (.stop @server)
                                             {:status 200}))
                              {:port 8080 :join? false}))
    c))

(defn get-access-token [{:keys [client-id client-secret code]}]
  (:body (client/post "https://oauth2.googleapis.com/token"
                      {:query-params {:client_id client-id
                                      :client_secret client-secret
                                      :code code
                                      :grant_type "authorization_code"
                                      :redirect_uri "http://localhost:8080"}
                       :as :json
                       :throw-entire-message? true})))

(defn revoke-token [token]
  (client/post "https://oauth2.googleapis.com/revoke" {:query-params {:token token}
                                                      :as :json
                                                      :throw-entire-message? true}))
(defn refresh-access-token [{:keys [client-id client-secret refresh-token]}]
  (get-in (client/post "https://oauth2.googleapis.com/token"
                       {:query-params
                        {:client_id client-id
                         :client_secret client-secret
                         :refresh_token refresh-token
                         :grant_type "refresh_token"}
                        :as :json})
          [:body]))

(defn valid-creds? []
  (and @creds (:expires-at @creds) (t/> (:expires-at @creds) (t/- (t/now) (t/new-duration 5 :minutes)))))

(defn reset-access-token! [{:keys [access_token expires_in]}]
  (:access-token
   (reset! creds
           {:access-token access_token
            :expires-at (t/+ (t/now) (t/new-duration expires_in :seconds))})))

(defn access-token [{:keys [client-id client-secret redirect-uri scope]}]
  (cond
    (valid-creds?)
    (:access-token @creds)
    
    (:refresh_token oauth-creds)
    (reset-access-token! (refresh-access-token {:client-id client-id
                                                :client-secret client-secret
                                                :refresh-token (:refresh_token oauth-creds)}))

    :else
    (let [token-chan (receive-token)]
      (sh "open" "-a" "firefox" "-g"
          (create-authorization-request {:client_id client-id :redirect_uri redirect-uri :scope scope}))
      (let [code (<!! token-chan)
            drive-api-credentials (get-access-token {:client-id client-id :client-secred client-secret :code code})]
        (spit oauth-creds-path (prn-str drive-api-credentials))
        (reset-access-token! drive-api-credentials)))))
(comment
  (def creds-file (read-string (slurp ".creds.edn")))
  (access-token {:client-id (:drive-client-id creds-file)
                 :client-secret (:drive-client-secret creds-file)
                 :redirect-uri "http://localhost:8080"
                 :scope ["https://www.googleapis.com/auth/drive"
                         "https://www.googleapis.com/auth/drive.file"]}))
