(ns tech.thomas-sojka.shopping-cards.util)

(defn days-in-month [month]
  (.getDate (js/Date. (.getFullYear (js/Date.)) month 0)))
