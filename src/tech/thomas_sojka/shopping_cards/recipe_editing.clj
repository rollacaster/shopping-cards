(ns tech.thomas-sojka.shopping-cards.recipe-editing
  (:require
   [clojure.string :as str]
   [tech.thomas-sojka.shopping-cards.data :as data]
   [tech.thomas-sojka.shopping-cards.scrape :as scrape]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-ingredient-to-recipe [recipe-ref
                                ingredient-ref
                                {:keys [amount amount-desc unit]}]
  (cond->
      #:cooked-with{:recipe recipe-ref
                    :ingredient ingredient-ref
                    :id (uuid)}
    amount-desc
    (assoc :cooked-with/amount-desc amount-desc)
    amount
    (assoc :cooked-with/amount amount)
    unit
    (assoc :cooked-with/unit unit)))

(defn add-ingredient [{:keys [category name]}]
  #:ingredient{:id (uuid),
               :name name,
               :category category})

(defn add-new-recipe [{:keys [name link image type ingredients]}]
  (cons
   {:db/id "new-recipe"
    :recipe/id (uuid),
    :recipe/name name,
    :recipe/type (keyword (str "recipe-type/" (str/lower-case type))),
    :recipe/image image,
    :recipe/link link}
   (map (fn [{:keys [amount amount-desc unit] :as ingredient}]
          (add-ingredient-to-recipe "new-recipe"
                                    [:ingredient/name (:name ingredient)]
                                    {:amount (when amount (float amount))
                                     :amount-desc amount-desc
                                     :unit unit}))
        ingredients)))

(defn remove-recipe [recipe-ref]
  (cons
   [:db/retractEntity recipe-ref]
   (map (fn [{:keys [id]}] [:db/retractEntity [:cooked-with/id id]])
        (:ingredients (data/load-recipe recipe-ref)))))

(comment
  (add-new-recipe
   {:name "Misosuppe mit Gemüse und Tofu2",
    :link "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
    :type "FAST",
    :inactive false,
    :image "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
    :ingredients [{:amount-desc "1 große", :name "Karotte", :amount 1, :id "960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9"}]})
  (add-ingredient {:category "Obst" :name "Mandarine"})
  (data/load-entity [:ingredient/name "Eier"])
  (-> {:name "temp" :link "https://www.meinestube.de/zucchini-frischkaese/"}
      scrape/scrape-recipe
      add-new-recipe))
