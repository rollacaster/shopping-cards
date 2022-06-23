(ns shadow
  (:require [shadow.cljs.devtools.api :as shadow]
            [shadow.cljs.devtools.server :as server]))

(defn -main [& _args]
  (server/start!)
  (shadow/watch :app)
  (let [dom-runtime-id (some
                        (fn [{:keys [dom client-id]}]
                          (when dom client-id))
                        (shadow/repl-runtimes :app))]
    (shadow/repl-runtime-select :app dom-runtime-id))
  (shadow/nrepl-select :app))
