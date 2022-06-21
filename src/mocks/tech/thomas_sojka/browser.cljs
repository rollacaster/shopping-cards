(ns tech.thomas-sojka.browser
  (:require
   ["msw" :as msw]
   [tech.thomas-sojka.handlers :refer [handlers]]))

(def worker (apply msw/setupWorker handlers))
