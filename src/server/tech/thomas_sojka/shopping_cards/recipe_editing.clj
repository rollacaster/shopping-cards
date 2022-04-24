(ns tech.thomas-sojka.shopping-cards.recipe-editing
  (:require
   [clojure.string :as str]
   [datomic.client.api :as d]
   [tech.thomas-sojka.shopping-cards.db :as db]
   [tech.thomas-sojka.shopping-cards.scrape :as scrape]))

;; URLS to implement
;; PUT /recipes/dc20d162-3405-4e41-8f58-34ebc7626124/ingredients/new
;; {ingredient-id: "cfc2741b-c361-4d05-b71e-a2a118881400"}
;; PUT /recipes/dc20d162-3405-4e41-8f58-34ebc7626124
;; {type: "RARE"}
;; POST /recipes/dc20d162-3405-4e41-8f58-34ebc7626124/ingredients/9580eac9-5902-4420-917d-7d6539c64c9b/inc
;; POST /recipes/dc20d162-3405-4e41-8f58-34ebc7626124/ingredients/9580eac9-5902-4420-917d-7d6539c64c9b/dec
;; DELETE /recipes/dc20d162-3405-4e41-8f58-34ebc7626124/ingredients/9580eac9-5902-4420-917d-7d6539c64c9b
(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn add-ingredient-to-recipe [recipe-ref ingredient-ref {:keys [amount amount-desc unit]}]
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
  (conj
   (mapv (fn [{:keys [amount amount-desc unit]}]
          (add-ingredient-to-recipe
           "new-recipe"
           "new-ingredient" ;; TODO How to handle existent ingredients
           {:amount (when amount (float amount))
            :amount-desc amount-desc
            :unit unit}))
        ingredients)
   {:db/id "new-recipe"
    :recipe/id (uuid),
    :recipe/name name,
    :recipe/type (keyword (str "recipe-type/" (str/lower-case type))),
    :recipe/image image,
    :recipe/link link}))

(defn update-recipe-type [conn request]
  (let [{:keys [type]} (:body-params request)
       {:keys [recipe-id]} (:params request)]
    (db/transact
     conn
     [{:db/id [:recipe/id recipe-id]
       :recipe/type (keyword (str "recipe-type/" (str/lower-case type)))}])
    {:status 200
     :body type}))

(defn remove-recipe [conn recipe-ref]
  (cons
   [:db/retractEntity recipe-ref]
   (map (fn [{:keys [id]}] [:db/retractEntity [:cooked-with/id id]])
        (:ingredients (db/load-recipe conn recipe-ref)))))

(comment
  (let [client (d/client {:server-type :dev-local :system "dev"})
        conn (d/connect client {:db-name "shopping-cards"})]
    (db/load-recipes conn)
    (update-recipe-type conn "38ba8e5b-61c7-4827-a848-45efd46717eb"
                        "RARE")
    (add-new-recipe
     {:name "Misosuppe mit Gemüse und Tofu2",
      :link "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
      :type "FAST",
      :inactive false,
      :image "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
      :ingredients [{:amount-desc "1 große", :name "Karotte", :amount 1, :id "960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9"}]})
    (add-ingredient {:category "Obst" :name "Mandarine"})
    (db/load-entity conn [:ingredient/name "Eier"])
    (-> {:name "temp" :link "https://www.meinestube.de/zucchini-frischkaese/"}
        (partial scrape/scrape-recipe conn)
        (partial add-new-recipe conn))))
