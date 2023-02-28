(ns shadow
  (:require [babashka.fs :as fs]
            [org.httpkit.server :as http]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.util.response :as response]
            [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]))
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
  (fs/copy "resources/test/index.html" "target/test/browser/index.html" {:replace-existing true})
  (reset! server (http/run-server (-> #'handler
                                      (wrap-file "target/test/browser" {:prefer-handler? true})
                                      wrap-content-type)
                                  {:port 9004})))
