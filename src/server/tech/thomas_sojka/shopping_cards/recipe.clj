(ns tech.thomas-sojka.shopping-cards.recipe
  (:require
   [clojure.string :as str]
   [tech.thomas-sojka.shopping-cards.db :as db]))

(defn edit [conn recipe-id recipe-upddate]
  (let [{:keys [type]} recipe-upddate
        new-recipe (cond-> {:db/id [:recipe/id recipe-id]}
                     type (assoc :recipe/type (keyword (str "recipe-type/" (str/lower-case type)))))]
    (db/transact conn [new-recipe])
    {:status 200 :body new-recipe}))
