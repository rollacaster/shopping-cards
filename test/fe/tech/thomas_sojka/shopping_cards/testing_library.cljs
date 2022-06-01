(ns tech.thomas-sojka.shopping-cards.testing-library
  (:require
   ["@testing-library/dom" :as tl]
   [cljs-bean.core :refer [->js]]
   [clojure.string :as str]))

(def wait-for tl/waitFor)

(defn get-by-role [container role options]
  (try
    (tl/getByRole container role (->js options))
    (catch :default e
      (let [err (first (str/split-lines (.-message e)))]
        (when-not (str/includes? err "Unable to find")
          (js/console.log (first (str/split-lines (.-message e))))))
      (throw e))))

(defn get-all-by-role [container role options]
  (tl/getAllByRole container role (->js options)))
