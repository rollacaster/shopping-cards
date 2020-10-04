(ns tech.thomas-sojka.shopping-cards.recipe-editing
  (:require [clj-http.client :as client]
            [clojure.set :refer [difference]]
            [clojure.string :as s]
            [clojure.core.async]
            [tech.thomas-sojka.shopping-cards.auth :refer [creds-file]]
            [tech.thomas-sojka.shopping-cards.db :as db]
            [tech.thomas-sojka.shopping-cards.scrape :as scrape]
            [tech.thomas-sojka.shopping-cards.trello :refer [trello-api]]))

(defn meal-line->clj [meal-line]
  (let [meal (apply str (drop 2 meal-line))]
    meal
    (if (s/starts-with? meal "[")
      (let [[_ meal-name link] (re-matches #"\[(.*)\]\((.*)\)" meal)]
        {:name meal-name :link link})
      {:name meal})))

(defn load-trello-recipes []
  (let [recipes-card-description
        (:desc (:body (client/get (str trello-api "/cards/" "OT6HW1Ik")
                                  {:query-params
                                   {:key (:trello-key creds-file)
                                    :token (:trello-token creds-file)}
                                   :as :json})))]
    (->> recipes-card-description
         s/split-lines
         (filter not-empty)
         (take-while #(not= % "Ideen"))
         (filter #(s/includes? % "- "))
         (map meal-line->clj))))

(defn added-recipes [trello-recipes]
  (let [added-names (difference
                     (set (map :name trello-recipes))
                     (set (map :name (db/load-recipes))))]
    (filter #(added-names (:name %)) (load-trello-recipes))))

(defn removed-recipes [trello-recipes]
  (difference
   (set (->> (db/load-recipes)
             (filter (comp not :inactive))
             (map :name)))
   (set (map :name trello-recipes))))

(defn mark-inactive-recipes [recipes remove-recipes]
  (db/write-edn "recipes.edn"
             (map #(assoc % :inactive (if (remove-recipes (:name %)) true false)) recipes)))

(defn show-recipe [recipe-id]
  (let [recipe (some (fn [{:keys [id] :as recipe}] (when (= id recipe-id) recipe))(db/load-recipes))
        cooked-with (filter #(= (:recipe-id %) (:id recipe)) (db/load-cooked-with))
        ingredients (map
                     #(some (fn [ingredient] (when (= (:ingredient-id %) (:id ingredient)) (merge ingredient %))) (db/load-ingredients))
                     cooked-with)]
    (assoc recipe :ingredients
           ingredients)))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-ingredient-to-recipe [recipe-id {:keys [amount category name amount-desc unit]}]
  (let [ingredient-id (uuid)]
    {:recipe-id recipe-id :amount-desc amount-desc
     :amount amount :unit unit :ingredient-id ingredient-id
     :id (uuid)}
    {:id ingredient-id :name name :category category}))

(defn add-ingredient [{:keys [category name]}]
  (db/write-edn
   "ingredients.edn"
   (conj
    (db/load-ingredients)
    {:id (uuid)
     :name name
     :category category})))

(defn find-ingredient [ingredient-name]
  (some #(when (= (:name %) ingredient-name) (:id %)) (db/load-ingredients)))

(defn add-cooked-with [recipe-id ingredient-id {:keys [amount amount-desc unit] :or {amount nil amount-desc "" unit nil}}]
  (db/write-edn
   "cooked-with.edn"
   (conj
    (db/load-cooked-with)
    {:recipe-id recipe-id :amount-desc amount-desc
     :amount amount :unit unit :ingredient-id ingredient-id
     :id (uuid)})))

(defn remove-cooked-with [recipe-id ingredient-id]
  (db/write-edn
   "cooked-with.edn"
   (remove
    #(and (= recipe-id (:recipe-id %)) (= (:ingredient-id %) ingredient-id))
    (db/load-cooked-with))))

(defn add-new-recipe [{:keys [name link image type] :as new-recipe}]
  (let [recipe-id (uuid)
        cooked-with (db/load-cooked-with)
        recipes (db/load-recipes)]
    (db/write-edn
     "cooked-with.edn"
     (->> new-recipe
          :ingredients
          (map
           (fn [{:keys [amount-desc amount id]}]
             {:id (uuid) :amount-desc amount-desc :amount amount :ingredient-id id :recipe-id recipe-id}))
          (concat cooked-with)
          vec))
    (db/write-edn "recipes.edn"
               (conj recipes {:id recipe-id :name name :link link :image image :type type}))))

(defn remove-ingredient [recipe ingredient-name]
  (update recipe :ingredients #(remove (fn [ingredient] (= ingredient-name (:name ingredient))) %)))

(defn find-recipe [recipe-name]
  (some #(when (= (:name %) recipe-name) (:id %)) (db/load-recipes)))

(defn missing-recipes []
  (let [missin-recipe-names (difference
                             (set (map :name (load-trello-recipes)))
                             (set(map :name (db/load-recipes))))]
    (filter #(missin-recipe-names (:name %)) (load-trello-recipes))))

(comment
  (missing-recipes)
  (add-cooked-with (find-recipe "Avocado-Pesto") (find-ingredient "Parmesan") {:amount nil :amount-desc "" :unit nil})
  (find-ingredient "Tomatenmark")
  (add-new-recipe
   (scrape/scrape-recipe
    {:link "https://www.chefkoch.de/rezepte/1660421274170785/Vegetarisches-Chili-mit-Bulgur.html?utm_source=net.whatsapp.WhatsApp.ShareExtension&utm_medium=Social%20Sharing%20CTA&utm_campaign=Sharing-iOS"
     :type "NORMAL"}))
  (add-ingredient {:category "Obst" :name "Kirsche"})
  (add-new-recipe
   {:inactive false,
    :name "Obst",
    :type "FAST",
    :ingredients '({:amount-desc "3",
                    :name "Äpfel",
                    :amount 1,
                    :id "2f989e52-c965-4f3c-af5b-e41ccd8b185f"}
                   {:amount-desc "1",
                    :name "Ananas",
                    :amount 1,
                    :id "fa465e7b-c158-40b5-8d16-d2a156c476c6"}
                   {:amount-desc "1",
                    :name "Wassermelone",
                    :amount 1,
                    :id "b287887d-346c-4644-ab84-4abc3eec81da"}
                   {:amount-desc "1 Packung",
                    :name "Trauben",
                    :amount 1,
                    :id "1ae57296-0493-4ac6-826e-549e4f4439a9"}
                   {:amount-desc "1 Packung",
                    :name "Blaubeeren",
                    :amount 1,
                    :id "8ce0dca4-db23-4ec8-a577-54e2caeeb802"}
                   {:amount-desc "1 Packung",
                    :name "Erdbeeren",
                    :amount 1,
                    :id "05da97fa-3bc1-40b5-a5bb-d1d341d96b44"}
                   {:amount-desc "1 Packung",
                    :name "Kirschen",
                    :amount 1,
                    :id "4caae747-2b97-4d1a-b477-629781beae3f"})})
  (add-new-recipe
   {:inactive false,
    :name "Vegetarische Reispfanne",
    :type "NORMAL",
    :link "https://www.chefkoch.de/rezepte/508081146067703/Vegetarische-Reispfanne.html?utm_source=net.whatsapp.WhatsApp.ShareExtension&utm_medium=Social%20Sharing%20CTA&utm_campaign=Sharing-iOS",
    :image "https://img.chefkoch-cdn.de/rezepte/508081146067703/bilder/297436/crop-360x240/vegetarische-reispfanne.jpg",
    :ingredients '({:amount-desc "1 Tasse/n",
                   :name "Reis",
                   :amount 1,
                   :id "9e0c19af-f27f-4b04-99fd-689357ee1be8"}
                   {:amount-desc "",
                    :name (find-ingredient "Tomatenmark"),
                    :amount nil,
                    :id "4e67d72f-44c4-464f-be33-05382c3c8080"}
                  {:amount-desc "2 Tasse/n",
                   :name "Gemüsebrühe",
                   :amount 2,
                   :id "4e67d72f-44c4-464f-be33-05382c3c8080"}
                  {:amount-desc "2 kleine",
                   :name "Zwiebel",
                   :amount 2,
                   :id "7cc3f4e2-fc7a-41d5-a2c8-65e53d9ad641"}
                  {:amount-desc "1",
                   :name "Paprika",
                   :amount 1,
                   :id "6c0740a2-24a0-4aa7-9548-60c79bac6fec"}
                  {:amount-desc "100 g",
                   :name "Feta",
                   :amount 100,
                   :id "d7d3faa8-f7c1-4cf2-ba0b-23df648b3c7c"}
                  {:amount-desc nil,
                   :name "Curry",
                   :amount nil,
                   :id "64e066ef-1e5b-4c67-aa34-acc6fee52ed8"}
                  {:amount-desc nil,
                   :name "Salz",
                   :amount nil,
                   :id "94f58d5a-221b-48d4-9f9e-118d1fdce128"}
                  {:amount-desc "3 TL",
                   :name "Öl",
                   :amount 3,
                   :id "3654b906-0dac-4db9-bf25-e4fbb9f4439f"})})
  (find-ingredient "Eier")
  (let [new-recipe (->> (load-trello-recipes)
                        added-recipes
                        #_(drop 1)
                        #_(take 1)
                        #_first
                        (map :name))]
    (scrape/find-image {:name "Obst"})
    (-> new-recipe
        (assoc :image "https://www.ditsch.de/mcinfo_assets/de/51d7fa67d7bf0a7db660b93b3530605eb3b222ff.jpeg")
        (assoc :ingredients [{:name "Spätzle" :amount 500 :amount-desc "500 g" :unit "g"}
                             {:name "Eier" :amount 2 :amount-desc nil :unit nil}])
        scrape/dedup-ingredients
        #_scrape/find-image))
  (add-ingredient {:category "Süßigkeiten" :name "Nachos"})
  (add-new-recipe
   (scrape/add-chefkoch-recipe {:link "https://www.chefkoch.de/rezepte/1631611270752104/Vegetarische-Frikadellen.html"
                                :type "NORMAL"})))
