(ns tech.thomas-sojka.shopping-cards.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [tick.core :refer [now]]))

(def trello-api "https://api.trello.com/1")
(def creds-file (read-string (slurp (io/resource ".creds.edn"))))

(defn load-edn [path] (read-string (slurp (io/resource path))))
(defn load-recipes [] (load-edn "recipes.edn"))
(defn load-ingredients [] (load-edn "ingredients.edn"))
(defn load-cooked-with [] (load-edn "cooked-with.edn"))

(defn ingredient-text [ingredients]
  (let [no-unit? (->> ingredients
                      (map :unit)
                      (every? nil?))
        no-amount? (->> ingredients
                        (map :amount-desc)
                        (every? nil?))
        all-amount? (->> ingredients
                        (map :amount)
                        (every? some?))
        name (:name (first ingredients))]
    (cond no-amount? name
          (= (count ingredients) 1) (str (:amount-desc (first ingredients)) " " name)
          (and no-unit? no-amount?) (str (float (reduce + (map :amount ingredients))) " " name)
          :else (str (count ingredients)
                     " " (:name (first ingredients))
                     " (" (s/join ", " (map :amount-desc ingredients)) ")"))))

(def penny-order
  ["Gemüse"
   "Gewürze"
   "Tiefkühl"
   "Brot & Co"
   "Müsli & Co"
   "Konserven"
   "Beilage"
   "Backen"
   "Fleisch"
   "Wursttheke"
   "Milch & Co"
   "Käse & Co"
   "Eier"
   "Getränke"])

(defn ingredients-for-recipes [selected-recipe-ids]
  (->> (load-cooked-with)
       (filter #(contains? selected-recipe-ids (:recipe-id %)))
       (map (fn [{:keys [ingredient-id amount-desc amount]}]
              (merge {:amount-desc amount-desc
                      :amount amount}
                     (some #(when (= (:id %) ingredient-id) %) (load-ingredients)))))
       (remove #(= (:category %) "Gewürze"))
       (remove #(= (:category %) "Backen"))
       (group-by :category)
       (sort-by :category (fn [a b] (< (.indexOf penny-order a) (.indexOf penny-order b))))
       (map second)
       flatten
       (group-by :id)
       (map (fn [[id ingredients]] (vector id (ingredient-text ingredients))))
       vec))

(defn category-ingredients->str [{:keys [ingredients]}]
  (->> ingredients
       (map #(str "- " %))
       (s/join "\n")))

(def klaka-board-id "48aas65T")

(defn load-trello-lists [board-id]
  (client/get (str trello-api "/boards/" board-id "/lists")
              {:query-params
               {:key (:trello-key creds-file)
                :token (:trello-token creds-file)}
               :as :json
               :throw-entire-message? true}))

(defn create-trello-shopping-card [list-id]
  (:body (client/post (str trello-api "/cards/")
                      {:query-params
                       {:key (:trello-key creds-file)
                        :token (:trello-token creds-file)
                        :name (str "Einkaufen " (apply str (take 10 (str (now)))))
                        :idList list-id}
                       :as :json
                       :throw-entire-message? true})))

(defn create-trello-checklist [card-id]
  (:body
   (client/post (str trello-api "/checklists")
                {:query-params
                 {:key (:trello-key creds-file)
                  :token (:trello-token creds-file)
                  :idCard card-id}
                 :as :json
                 :throw-entire-message? true})))

(defn create-trell-checklist-item [checklist-id item]
  (:body (client/post (str trello-api (str "/checklists/" checklist-id "/checkItems"))
                      {:query-params
                       {:key (:trello-key creds-file)
                        :token (:trello-token creds-file)
                        :name item}
                       :as :json
                       :throw-entire-message? true})))

(defn create-klaka-shopping-card [ingredients]
  (let [list-id (:id (first (:body (load-trello-lists klaka-board-id)))) ;; TODO Replace with Klaka-Id
        card-id (:id (create-trello-shopping-card list-id))
        checklist-id (:id (create-trello-checklist card-id))]
    (doseq [ingredient ingredients]
      (create-trell-checklist-item checklist-id ingredient))
    card-id))







