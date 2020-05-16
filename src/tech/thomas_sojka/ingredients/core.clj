(ns tech.thomas-sojka.ingredients.core
  (:require [clj-http.client :as client]
            [clojure.string :as s]
            [clojure.walk :as w]
            [hickory.core :as html]
            [hickory.select :as select]))

(def trello-api "https://api.trello.com/1")
(def board-url "https://api.trello.com/1/members/me/boards")
(def creds-file (read-string (slurp ".creds.edn")))

(def res (:body (client/get (str trello-api "/cards/" "OT6HW1Ik")
                            {:query-params {:key (:trello-key creds-file) :token (:trello-token creds-file)} :as :json})))

(defn meal-line->clj [meal-line]
  (let [meal (apply str (drop 2 meal-line))]
    meal
    (if (s/starts-with? meal "[")
      (let [[_ meal-name link] (re-matches #"\[(.*)\]\((.*)\)" meal)]
        {:name meal-name :link link})
      {:name meal})))

(def recipes
  (->> (:desc res)
       s/split-lines
       (filter not-empty)
       (take-while #(not= % "Schnell Gerichte"))
       (map meal-line->clj)))

(defn is-link? [node]
  (= (:tag node) :a))

(defn transform-link [node]
  (first (:content node)))

(defn walk [node]
  (cond
    (is-link? node) (transform-link node)
    :else node))

(defn split-on-space [word]
  (clojure.string/split word #"\s"))

(defn trim-all [word]
  (->> word
       split-on-space
       (filter #(not (clojure.string/blank? %)))
       (clojure.string/join " ")))

(defn parse-int [s]
  (when (and s (re-find #"\d+" s))
    (Integer. (re-find #"\d+" s))))

(defn scrape-chefkoch-ingredients [link]
  (let [recipe-html (:body (client/get link))]
    (->> recipe-html
         html/parse
         html/as-hickory
         (select/select
          (select/child
           (select/class "ingredients")
           (select/tag :tbody)
           (select/tag :tr)
           (select/tag :td)))
         (map (comp :content first #(filter (complement string?) %) :content))
         (w/postwalk walk)
         (map first)
         (map #(if (string? %) (trim-all %) %))
         (partition 2)
         (map #(zipmap [:amount-desc :name] %))
         (map #(assoc % :amount (parse-int (:amount-desc %)))))))


