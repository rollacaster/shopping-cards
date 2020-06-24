(ns tech.thomas-sojka.shopping-cards.util
  (:require clojure.java.io
            clojure.pprint))

(defn write-edn [path data]
  (binding [*print-level* nil
            *print-length* nil]
    (clojure.pprint/pprint data (clojure.java.io/writer (str "resources/" path)))))
