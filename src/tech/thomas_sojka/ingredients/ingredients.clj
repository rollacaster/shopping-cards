(ns tech.thomas-sojka.ingredients.ingredients
  (:require [clj-fuzzy.metrics :as fuzzy]
            [clojure.math.combinatorics :as combo]
            [clojure.set :as set]))

(defn- do-cross-sets [current others]
  (let [union (set/union current (first others))
        rest (drop 1 others)]
    (if (empty? others)
      current
      (if (=
            (count union)
            (+ (count current) (count (first others))))
        (do-cross-sets current rest)
        (do-cross-sets union rest)))))

(defn- remove-subsets [current others]
  (if (some #(set/subset? current %) others)
    nil
    current))

(defn merge-intersecting-sets [sets]
  (let [cross-sets (set (for [x sets]
                          (do-cross-sets x (remove #{x} sets))))]
    (filter some? (for [x cross-sets]
                    (remove-subsets x (remove #{x} cross-sets))))))

(defn possible-duplicated-ingredients [recipes]
  (let [ingredients (->> recipes
                         (map :ingredients)
                         flatten
                         (filter some?)
                         (map :name)
                         distinct)
        ingredient-combos (combo/combinations ingredients 2)
        duplicated-ingredients (->> ingredient-combos
                                    (filter #(> (apply fuzzy/dice %) 0.56))
                                    (map set)
                                    merge-intersecting-sets)]
    duplicated-ingredients))
