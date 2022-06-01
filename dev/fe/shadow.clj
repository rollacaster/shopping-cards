(ns shadow
  (:require [org.httpkit.server :as http]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file :refer [wrap-file]]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]
            [ring.util.response :as response]))
(def server (atom nil))

(defn handler [req]
  (if (= (:uri req) "/")
    (assoc-in (response/file-response "target/test/browser/index.html") [:headers "Content-Type"] "text/html")
    (assoc req :status 404)))

(defn -main [& _args]
  (server/start!)
  (shadow/watch :app)
  (let [dom-runtime-id (some
                        (fn [{:keys [dom client-id]}]
                          (when dom client-id))
                        (shadow/repl-runtimes :app))]
    (shadow/repl-runtime-select :app dom-runtime-id))
  (shadow/watch :test)
  (reset! server (http/run-server (-> #'handler
                                      (wrap-file "target/test/browser" {:prefer-handler? true})
                                      wrap-content-type)
                                  {:port 9001})))
