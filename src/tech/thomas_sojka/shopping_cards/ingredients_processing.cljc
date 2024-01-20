(ns tech.thomas-sojka.shopping-cards.ingredients-processing
  (:require [clojure.string :as str]))

(def penny-order
  [:ingredient-category/obst
   :ingredient-category/gemüse
   :ingredient-category/gewürze
   :ingredient-category/tiefkühl
   :ingredient-category/brot&co
   :ingredient-category/müsli&co
   :ingredient-category/konserven
   :ingredient-category/beilage
   :ingredient-category/backen
   :ingredient-category/fleisch
   :ingredient-category/wursttheke
   :ingredient-category/milch&co
   :ingredient-category/käse&co
   :ingredient-category/süßigkeiten
   :ingredient-category/eier
   :ingredient-category/getränke])

(defn- ingredient-text [ingredients]
  (let [no-unit?
        (->> ingredients
             (map (fn [[{:keys [cooked-with/unit]}]] unit))
             (every? nil?))
        amount-descs
        (map (fn [[{:keys [cooked-with/amount-desc]}]] amount-desc) ingredients)
        amounts
        (map (fn [[{:keys [cooked-with/amount]}]] amount) ingredients)
        no-amount? (every? nil? amount-descs)
        {:keys [cooked-with/amount-desc]} (ffirst ingredients)
        {:keys [ingredient/name]} (second (first ingredients))]
    (str/trim
     (cond no-amount? name
           (= (count ingredients) 1) (str amount-desc " " name)
           (and no-unit? no-amount?) (str (float (reduce + amounts)) " " name)
           :else (str (count ingredients)
                      " " name
                      " (" (str/join ", " amount-descs) ")")))))

(defn process-ingredients [ingredients]
  (->> ingredients
       (group-by (fn [[_ {:keys [ingredient/id]}]] id))
       (sort-by (fn [[_ [[_ {{:keys [db/ident]} :ingredient/category}]]]]
                  ident)
                (fn [category1 category2]
                  (< (.indexOf penny-order category1)
                     (.indexOf penny-order category2))))
       (map (fn [[_ ingredients]]
              (let [{:keys [ingredient/id]} (second (first ingredients))]
                [id (ingredient-text ingredients)])))))
