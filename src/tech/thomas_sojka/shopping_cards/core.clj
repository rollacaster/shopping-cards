(ns tech.thomas-sojka.shopping-cards.core
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [tech.thomas-sojka.shopping-cards.util :refer [write-edn]]
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
       (group-by :id)
       (sort-by second (fn [[a] [b]]
                         (prn (:category a))
                        (< (.indexOf penny-order (:category a)) (.indexOf penny-order (:category b)))))
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

(comment
  (defn show-recipe [recipe-id]
    (let [recipe (some (fn [{:keys [id] :as recipe}] (when (= id recipe-id) recipe))(load-recipes))
          cooked-with (filter #(= (:recipe-id %) (:id recipe)) (load-cooked-with))
          ingredients (map
                       #(some (fn [ingredient] (when (= (:ingredient-id %) (:id ingredient)) (merge ingredient %))) (load-ingredients))
                       cooked-with)]
      (assoc recipe :ingredients
             ingredients)))

  (defn uuid [] (str (java.util.UUID/randomUUID)))

  (defn add-ingredient [recipe-id {:keys [amount category name amount-desc unit]}]
    (let [ingredient-id (uuid)]
      {:recipe-id recipe-id :amount-desc amount-desc
       :amount amount :unit unit :ingredient-id ingredient-id
       :id (uuid)}
      {:id ingredient-id :name name :category category}))

  (defn find-ingredient [ingredient-name]
    (some #(when (= (:name %) ingredient-name) (:id %)) (load-ingredients)))

  (defn add-cooked-with [recipe-id ingredient-id {:keys [amount amount-desc unit] :or {amount nil amount-desc "" unit nil}}]
    (write-edn
     "cooked-with.edn"
     (conj
      (load-cooked-with)
      {:recipe-id recipe-id :amount-desc amount-desc
       :amount amount :unit unit :ingredient-id ingredient-id
       :id (uuid)})))

  (defn remove-cooked-with [recipe-id ingredient-id]
    (write-edn
     "cooked-with.edn"
     (remove
      #(and (= recipe-id (:recipe-id %)) (= (:ingredient-id %) ingredient-id))
      (load-cooked-with))))
  (load-recipes)
  (def recipe-id "dd3fa340-a54a-4dc8-aea2-68cdc3656608")
  (show-recipe recipe-id)
  (map ingredient-text (map vector (:ingredients (show-recipe recipe-id))))
  (add-cooked-with recipe-id (find-ingredient "Milde Peperoni") {:amount-desc "" :amount nil :unit nil})
  (remove-cooked-with recipe-id (find-ingredient "Milde Peperoni"))
  ;; todo remove ingredients championgons
  #_(add-ingredient "dd3fa340-a54a-4dc8-aea2-68cdc3656608" {}))









