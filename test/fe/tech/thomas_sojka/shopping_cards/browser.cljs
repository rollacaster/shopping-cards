(ns tech.thomas-sojka.shopping-cards.browser
  (:require
   ["msw" :as msw]
   [tech.thomas-sojka.shopping-cards.handlers :refer [handlers]]))

(def worker (apply msw/setupWorker handlers))
