(ns tech.thomas-sojka.handlers
  (:require ["msw" :as msw]
            [cognitect.transit :as t]
            [tech.thomas-sojka.shopping-cards.queries :as queries])
  (:require-macros [tech.thomas-sojka.handlers :as handlers]))


(def enums
  [#:db{:ident :ingredient-category/backen}
   #:db{:ident :ingredient-category/beilage}
   #:db{:ident :ingredient-category/brot&co}
   #:db{:ident :ingredient-category/eier}
   #:db{:ident :ingredient-category/fleisch}
   #:db{:ident :ingredient-category/gemüse}
   #:db{:ident :ingredient-category/getränke}
   #:db{:ident :ingredient-category/gewürze}
   #:db{:ident :ingredient-category/konserven}
   #:db{:ident :ingredient-category/käse&co}
   #:db{:ident :ingredient-category/milch&co}
   #:db{:ident :ingredient-category/müsli&co}
   #:db{:ident :ingredient-category/obst}
   #:db{:ident :ingredient-category/süßigkeiten}
   #:db{:ident :ingredient-category/tiefkühl}
   #:db{:ident :ingredient-category/wursttheke}
   #:db{:ident :recipe-type/fast}
   #:db{:ident :recipe-type/misc}
   #:db{:ident :recipe-type/new}
   #:db{:ident :recipe-type/normal}
   #:db{:ident :recipe-type/rare}])

(def ingredients
  [#:ingredient{:id "dc1b7bdc-9f9e-4751-b935-468919d39030",
                :name "Carrot",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "bbaba432-0330-4043-90c6-3d3df2fac57b",
                :name "Onion",
                :category :ingredient-category/gemüse}
   #:ingredient{:id "e0fb50e5-ec7f-4b80-9456-e9f9d2fbd68f",
                :name "Mushroom",
                :category :ingredient-category/gemüse}])

(def recipes
  [[{:id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
     :link
     "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
     :image
     "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
     :recipe/type {:db/ident :recipe-type/fast},
     :name "Soup"}]])

(def cooked-with [#:cooked-with{:ingredient [:ingredient/name "Carrot"],
                                :id "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
                                :recipe [:recipe/name "Soup"],
                                :amount-desc "1",
                                :amount 1.0}
                  #:cooked-with{:ingredient [:ingredient/name "Onion"],
                                :id "3541b429-879e-419b-8597-e2451f1d4acf",
                                :recipe [:recipe/name "Soup"],
                                :amount-desc "1",
                                :amount 1.0}
                  #:cooked-with{:ingredient [:ingredient/name "Mushroom"],
                                :id "60e71736-c2b2-4108-8a17-a5894a213786",
                                :recipe [:recipe/name "Soup"],
                                :amount-desc "1",
                                :amount 1.0}])

(def w (t/writer :json))
(def r (t/reader :json))

(def handlers [(msw/rest.post "/query" (fn [req res ctx]
                                         (let [query (:q (t/read r (js/JSON.stringify (.-body req))))]
                                           (js/console.log "query" query)
                                           (cond
                                             (= query queries/load-recipes)
                                             (res
                                              (.status ctx 200)
                                              (.text ctx (t/write w recipes)))
                                             (= query queries/load-ingredients-by-recipe-id)
                                             (res
                                              (.status ctx 200)
                                              (.text ctx
                                                     (t/write w (mapv
                                                                 (fn [{:cooked-with/keys [amount amount-desc id unit ingredient]}]
                                                                   [(cond->
                                                                        {:cooked-with/amount amount
                                                                         :cooked-with/amount-desc amount-desc
                                                                         :cooked-with/id id}
                                                                      unit (assoc :cooked-with/unit unit))
                                                                    (let [{:ingredient/keys [category name id]}
                                                                          (some #(when (= (:ingredient/name %) (second ingredient)) %) ingredients)]
                                                                      {:ingredient/name name
                                                                       :ingredient/category category
                                                                       :ingredient/id id})])
                                                                 cooked-with))))
                                             :else
                                             (res
                                              (.status ctx 200))))))
               (msw/rest.put "/transact"
                             (fn [req res ctx]
                               (let [tx-data (t/read r (js/JSON.stringify (.-body req)))]
                                 (js/console.log "transact" tx-data)
                                 (res
                                  (.status ctx 200)))))
               (msw/rest.post "/shopping-card"
                              (fn [_ res ctx]
                                (res
                                 (.status ctx 200)
                                 (.text ctx "fake-trello-card-id"))))
               (msw/rest.get (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                                  (.getFullYear (js/Date.))
                                  ".edn")
                             (fn [_ res ctx]
                               (res
                                (.status ctx 200)
                                (.text ctx (handlers/bank-holidays)))))])
