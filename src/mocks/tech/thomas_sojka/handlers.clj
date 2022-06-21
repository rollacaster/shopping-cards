(ns tech.thomas-sojka.handlers)

(defmacro tachyons-css []
  (slurp "https://unpkg.com/tachyons@4.12.0/css/tachyons.min.css"))

(defmacro styles []
  (slurp "resources/public/css/styles.css"))

(defmacro bank-holidays []
  (slurp (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/" (.getYear (java.time.LocalDateTime/now)) ".edn")))
