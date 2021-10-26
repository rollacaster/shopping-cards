(ns tech.thomas-sojka.shopping-cards.util)

(defn days-in-current-month []
  (.getDate (js/Date. (.getFullYear (js/Date.)) (inc (.getMonth (js/Date.))) 0)))
