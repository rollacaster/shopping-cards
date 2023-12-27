(ns clerk
  (:require [nextjournal.clerk :as clerk]))

(defn -main [& _args]
  (clerk/serve! {:browse true}))
