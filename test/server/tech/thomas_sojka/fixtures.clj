(ns tech.thomas-sojka.fixtures
  (:require
   [datomic.client.api :as d]
   [integrant.core :as ig]
   [integrant.repl :as ig-repl]
   [tech.thomas-sojka.shopping-cards.db :as db]
   [tech.thomas-sojka.shopping-cards.system]))

(def ingredients
  [#:ingredient{:id "690fdb5c-711b-4b1b-918b-148d2a4eb355",
                :name "Spinat",
                :category :ingredient-category/tiefkühl}
   #:ingredient{:id "2afef478-85f4-4e5c-baeb-b04f48e4a945",
                :name "Spätzle",
                :category :ingredient-category/milch&co}
   #:ingredient{:id "2a647d9b-4a02-4853-bad4-ca0f9201ed8b",
                :name "Eier",
                :category :ingredient-category/eier}
   #:ingredient{:id "be78e544-68c8-4a06-89ba-6def6d88152d",
                :name "Sonnenblumenkerne",
                :category :ingredient-category/müsli&co}
   #:ingredient{:id "e6ce2fbe-8f6b-442e-a9e3-cdb67a1c90a1",
                :name "Mehl",
                :category :ingredient-category/backen}
   #:ingredient{:id "2dee32c2-1b68-4eea-a80d-5c5668a24d45",
                :name "Nachos",
                :category :ingredient-category/süßigkeiten}
   #:ingredient{:id "6175d1a2-0af7-43fb-8a53-212af7b72c9c",
                :name "Wasser",
                :category :ingredient-category/getränke}
   #:ingredient{:id "cfc2741b-c361-4d05-b71e-a2a118881400",
                :name "Mandeln",
                :category :ingredient-category/obst}
   #:ingredient{:id "f365f293-4adc-4cdc-bcca-389425f3e2e6",
                :name "Lasagneplatten",
                :category :ingredient-category/beilage}
   #:ingredient{:id "e0f74a51-d1b7-46f5-b0ad-d5b64de3d24b",
                :name "Salami",
                :category :ingredient-category/wursttheke}
   #:ingredient{:id "dc1b7bdc-9f9e-4751-b935-468919d39030",
                :name "Porree",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "24a03356-60cd-4f92-9f79-4cc511dd6d7e",
                :name "Gorgonzola",
                :category :ingredient-category/käse&co}
   #:ingredient{:id "9edc6e43-a040-4829-88f8-de0eaa0a5209",
                :name "Currypaste",
                :category :ingredient-category/gewürze}
   #:ingredient{:id "f29557a1-d028-4efa-860d-562ad6fe8c56",
                :name "Wraps",
                :category :ingredient-category/brot&co}
   #:ingredient{:id "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd",
                :name "Passierte Tomaten",
                :category :ingredient-category/konserven}
   #:ingredient{:id "c31b04d7-8009-4a63-935e-6185b226280e",
                :name "Hackfleisch",
                :category :ingredient-category/fleisch}
   #:ingredient{:id "61858d61-a9d0-4ba6-b341-bdcdffec50d1"
                :name "Mandarine",
                :category :ingredient-category/obst}])

(def recipes
  [{:recipe/id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
    :recipe/link
    "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
    :recipe/image
    "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
    :recipe/type :recipe-type/fast,
    :recipe/name "Misosuppe mit Gemüse und Tofu2"}])

(def cooked-with [#:cooked-with{:ingredient [:ingredient/name "Mandarine"],
                                :id "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
                                :recipe [:recipe/name "Misosuppe mit Gemüse und Tofu2"],
                                :amount-desc "1 große",
                                :amount 1.0}])

(def db-name "shopping-cards-test")
(def port 3001)

(defn url [& endpoint]
  (apply str "http://localhost:" port endpoint))

(def config
  {:adapter/jetty {:port port
                   :trello-client (ig/ref :external/trello-client)
                   :conn (ig/ref :datomic/dev-local)}
   :external/trello-client {}
   :datomic/dev-local {:db-name db-name}})

(defn- populate []
  (let [client (d/client {:server-type :dev-local :system "dev"})
        conn (d/connect client {:db-name db-name})]
    (db/transact conn ingredients)
    (db/transact conn recipes)
    (db/transact conn cooked-with)))

(defn db-setup [test-run]
  (ig-repl/set-prep! (fn [] config))
  (ig-repl/go)
  (populate)
  (test-run)
  (d/delete-database (d/client {:server-type :dev-local :system "dev"})
                     {:db-name db-name})
  (ig-repl/halt))

(comment
  (do
    (ig-repl/halt)
    (ig-repl/go)
    (populate)))
