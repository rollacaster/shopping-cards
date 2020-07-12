(ns tech.thomas-sojka.shopping-cards.db
  (:require [clojure.java.io :as io]
            clojure.pprint))

(defn load-edn [path] (read-string (slurp (io/resource path))))
(defn load-recipes [] (load-edn "recipes.edn"))
(defn load-ingredients [] (load-edn "ingredients.edn"))
(defn load-cooked-with [] (load-edn "cooked-with.edn"))

(defn write-edn [path data]
  (binding [*print-level* nil
            *print-length* nil]
    (clojure.pprint/pprint data (clojure.java.io/writer (str "resources/" path)))))
