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
  [[{:id "2aa44c10-bf40-476b-b95f-3bbe96a3835f",
     :link
     "https://www.chefkoch.de/rezepte/1073731213081387/Misosuppe-mit-Gemuese-und-Tofu.html",
     :image
     "https://img.chefkoch-cdn.de/rezepte/1073731213081387/bilder/1319791/crop-360x240/misosuppe-mit-gemuese-und-tofu.jpg",
     :recipe/type {:db/ident :recipe-type/fast},
     :name "Misosuppe mit Gemüse und Tofu2"}]])

(def cooked-with [#:cooked-with{:ingredient [:ingredient/name "Mandarine"],
                                :id "ab52a4b5-46c3-4d1e-9e42-a66a02e19ba9",
                                :recipe [:recipe/name "Misosuppe mit Gemüse und Tofu2"],
                                :amount-desc "1 große",
                                :amount 1.0}])

(def w (t/writer :json))
(def r (t/reader :json))

(def handlers [(msw/rest.post "/query" (fn [req res ctx]
                                         (let [query (:q (t/read r (js/JSON.stringify (.-body req))))]
                                           (cond
                                             (= query queries/load-recipes)
                                             (res
                                              (.status ctx 200)
                                              (.text ctx (t/write w recipes)))
                                             :else
                                             (res
                                              (.status ctx 200))))))
               (msw/rest.get "/styles.css" (fn [_ res ctx]
                                              (res
                                               (.status ctx 200)
                                               (.text ctx (handlers/styles)))))
               (msw/rest.get "https://unpkg.com/tachyons@4.12.0/css/tachyons.min.css"
                             (fn [_ res ctx]
                               (res
                                (.status ctx 200)
                                (.text ctx (handlers/tachyons-css)))))
               (msw/rest.get (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                                  (.getFullYear (js/Date.))
                                  ".edn")
                             (fn [_ res ctx]
                               (res
                                (.status ctx 200)
                                (.text ctx (handlers/bank-holidays)))))])
