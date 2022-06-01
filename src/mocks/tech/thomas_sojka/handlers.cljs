(ns tech.thomas-sojka.handlers
  (:require ["msw" :as msw]
            [cognitect.transit :as t]
            [tech.thomas-sojka.shopping-cards.fixtures :as fixtures]
            [tech.thomas-sojka.shopping-cards.queries :as queries])
  (:require-macros [tech.thomas-sojka.handlers :as handlers]))


(def recipes
  [[{:id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
     :link
     "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
     :image
     "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
     :recipe/type {:db/ident :recipe-type/fast},
     :name "Soup"}]])

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
                                                                          (some #(when (= (:ingredient/name %) (second ingredient)) %) fixtures/ingredients)]
                                                                      {:ingredient/name name
                                                                       :ingredient/category category
                                                                       :ingredient/id id})])
                                                                 fixtures/cooked-with))))
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
                                (.text ctx "#{}"))))])
