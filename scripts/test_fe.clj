(ns test-fe
  (:require [clojure.java.shell :as shell]))

(defn run-test-suite!
  "Run the full test suite of the project. Will exit with a non zero code if
  there is any failure. Otherwise exits with a zero exit code."
  []
  (print "Compiling tests...")
  (flush)
  (let [{:keys [out err]} (shell/sh "npx" "shadow-cljs" "-A:test" "compile" "ci")]
    (println " ✅")
    (println out)
    (println err))
  (print "Running tests...")
  (flush)
  (let [{:keys [out exit]} (shell/sh "npx" "karma" "start" "--single-run")
        success? (re-find #"(?m)TOTAL: \d SUCCESS" out)]
    (flush)
    (println (str " " (if success? "✅" "❌")))
    (println out)
    (System/exit exit)))
