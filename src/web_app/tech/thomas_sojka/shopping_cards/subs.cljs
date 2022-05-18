(ns tech.thomas-sojka.shopping-cards.subs
  (:require [re-frame.core :refer [reg-sub] :as rf]))

(reg-sub
 :app/error
 (fn [db _]
   (:app/error db)))

(reg-sub
 :app/route
 (fn [db _]
   (:app/route db)))

(reg-sub
 :app/loading
 (fn [db _]
   (:app/loading db)))

(reg-sub
 :db/conn
 (fn [db _]
   (:db/conn db)))
