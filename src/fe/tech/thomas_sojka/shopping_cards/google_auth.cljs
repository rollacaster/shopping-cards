(ns tech.thomas-sojka.shopping-cards.google-auth
  (:require ["date-fns" :refer (isAfter subMinutes addSeconds)]
            [child_process :as child-process]
            [clojure.edn :as edn]
            [clojure.string :as s]
            [fs :as fs]
            [http]))

(def creds-file (edn/read-string (.toString (fs/readFileSync "resources/.creds.edn"))))
(def oauth-creds-path "resources/.google-oauth-creds")
(def oauth-creds
  (try
    (edn/read-string (.toString (fs/readFileSync oauth-creds-path)))
    (catch :default _ nil)))

(def creds (atom nil))
(def code (atom nil))
(def server (atom nil))

(defn query-params [params]
  (str "?"
       (->> params
            (map (fn [[key value]] (str (name key) "=" (if (coll? value) (s/join " " value) value))))
            (s/join "&"))))

(defn create-authorization-request [params]
  (str "https://accounts.google.com/o/oauth2/v2/auth" (query-params (assoc params :access_type "offline" :response_type "code"))))

(defn get-code [req]
  (.get (.-searchParams (new js/URL (str "http://localhost" (.-url req)))) "code"))

(defn receive-token []
  (reset! server (.createServer http
                                (fn [req res]
                                  (reset! code (get-code req))
                                  (.writeHead ^js res 200)
                                  (.end res)
                                  (.close @server))))
  (.listen ^js @server 8080))

(defn get-access-token [{:keys [client-id client-secret code]}]
  (str "https://oauth2.googleapis.com/token"
       (query-params {:client_id client-id
                      :client_secret client-secret
                      :code code
                      :grant_type "authorization_code"
                      :redirect_uri "http://localhost:8080"})))

(defn refresh-access-token [{:keys [client-id client-secret refresh-token]}]
  (str "https://oauth2.googleapis.com/token"
       (query-params {:client_id client-id
                      :client_secret client-secret
                      :refresh_token refresh-token
                      :grant_type "refresh_token"})))

(defn valid-creds? []
  (and @creds (:expires-at @creds) (isAfter (:expires-at @creds) (subMinutes (js/Date.) 5))))

(defn reset-access-token! [{:keys [access_token expires_in]}]
  (:access-token
   (reset! creds
           {:access-token access_token
            :expires-at (addSeconds (js/Date.) expires_in)})))

(defn access-token [{:keys [client-id client-secret redirect-uri scope]}]
  (cond
    (valid-creds?)
    (js/Promise.resolve (:access-token @creds))

    (:refresh_token oauth-creds)
    (-> (js/fetch (refresh-access-token {:client-id client-id
                                         :client-secret client-secret
                                         :refresh-token (:refresh_token oauth-creds)})
                  #js {:method "POST"})
        (.then (fn [res] (.json res)))
        (.then (fn [access-token]
                 (reset-access-token! (js->clj access-token :keywordize-keys true)))))

    :else
    (do
      (receive-token)
      (child-process/exec (str "open -a firefox -g '" (create-authorization-request {:client_id client-id :redirect_uri redirect-uri :scope scope}) "'"))
      (-> {:client-id client-id :client-secret client-secret :code code}
          get-access-token
          (js/fetch #js {:method "POST"})
          (.then (fn [drive-api-credentials]
                   (fs/writeFileSync oauth-creds-path (prn-str drive-api-credentials))
                   (reset-access-token! drive-api-credentials)))))))

(comment
  (access-token {:client-id (:drive-client-id creds-file)
                 :client-secret (:drive-client-secret creds-file)
                 :redirect-uri "http://localhost:8080"
                 :scope ["https://www.googleapis.com/auth/drive"
                         "https://www.googleapis.com/auth/drive.file"]}))
