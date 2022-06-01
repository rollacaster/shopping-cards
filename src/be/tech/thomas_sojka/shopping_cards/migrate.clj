(ns tech.thomas-sojka.shopping-cards.migrate
  (:require [datomic.client.api :as d]
            [hodur-datomic-schema.core :as hodur-datomic]
            [tech.thomas-sojka.shopping-cards.db :as db]
            [tech.thomas-sojka.shopping-cards.schema :refer [meta-db]]))

(defn migrate-schema [conn]
  (let [datomic-schema (hodur-datomic/schema meta-db)]
    (d/transact conn {:tx-data datomic-schema})
    (d/transact conn {:tx-data [{:db/ident :cooked-with/recipe+ingredient
                                 :db/valueType :db.type/tuple
                                 :db/tupleAttrs [:cooked-with/ingredient :cooked-with/recipe]
                                 :db/cardinality :db.cardinality/one
                                 :db/unique :db.unique/identity}]})
    (d/transact conn {:tx-data [{:db/ident :meal-plan/inst+type
                                 :db/valueType :db.type/tuple
                                 :db/tupleAttrs [:meal-plan/inst :meal-plan/type]
                                 :db/cardinality :db.cardinality/one
                                 :db/unique :db.unique/identity}]})))

(defn update-unit [conn props]
  (let [recipe-name (:recipe/name props)
        ingredient-name (:ingredient/name props)
        cooked-with-updates (dissoc props :recipe/name :ingredient/name)
        eid (->> (d/q '[:find ?e
                        :in $ ?recipe-name ?ingredient-name
                        :where
                        [?e :cooked-with/recipe ?r]
                        [?e :cooked-with/ingredient ?i]
                        [?r :recipe/name ?recipe-name]
                        [?i :ingredient/name ?ingredient-name]]
                      (d/db conn)
                      recipe-name
                      ingredient-name)
                 ffirst)]
    (merge {:db/id eid} cooked-with-updates)))

(def cooked-with-migrations [{:recipe/name "Obst"
                              :ingredient/name "Griechischer Joghurt"
                              :cooked-with/unit "Packung"}
                             {:recipe/name "Vegetarisches Chili mit Bulgur"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Falafel-Wraps"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Raclette im Ofen"
                              :ingredient/name "Mais"
                              :cooked-with/unit "g"}
                             {:recipe/name "Brokkoli-Nudelauflauf"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Quesadilla"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Couscous"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Pide"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Flammkuchen vegetarisch"
                              :ingredient/name "Mais"
                              :cooked-with/unit "Dose"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Nudelsalat mit Erbsen"
                              :ingredient/name "Joghurt"
                              :cooked-with/unit "g"}
                             {:recipe/name "Indisches Kichererbsen-Curry"
                              :ingredient/name "Joghurt"
                              :cooked-with/unit "EL"}
                             {:recipe/name "Obst"
                              :ingredient/name "Skyr"
                              :cooked-with/unit "Packung"}
                             {:recipe/name "Indisches Kichererbsen-Curry"
                              :ingredient/name "Kreuzkümmel"
                              :cooked-with/unit "TL"}
                             {:recipe/name "Gnocchi mit Brokkoli und Pesto Rosso"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Cremiger Nudelauflauf"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Schinkennudeln vegetarisch"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Kohlrabi in Parmesan-Kräuter-Panade"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Cremige Käse-Lauch-Pasta"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Kichererbsen & Spinat Nudeln"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Spinatlasagne"
                              :ingredient/name "Parmesan"
                              :cooked-with/unit "g"}
                             {:recipe/name "Cremiger Nudelauflauf"
                              :ingredient/name "Parmesan"
                              :cooked-with/amount-desc "4 EL"}
                             {:recipe/name "Indisches Kichererbsen-Curry"
                              :ingredient/name "Kichererbsen"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Vegetarisches Chili mit Bulgur"
                              :ingredient/name "Kichererbsen"
                              :cooked-with/unit "Dose"}
                             {:recipe/name "Kichererbsen & Spinat Nudeln"
                              :ingredient/name "Kichererbsen"
                              :cooked-with/unit "g"}
                             {:recipe/name "Gebackener Feta"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Vegetarische Reispfanne"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Spaghetti mit rotem Pesto und Schafskäse"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Gefüllte Paprika mit Feta und Mozarella"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Zucchinipuffer mit Fetakäse"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Teigtaschen mit Spinat-Feta-Füllung"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Spinat-Feta Nudelauflauf"
                              :ingredient/name "Feta"
                              :cooked-with/unit "g"}
                             {:recipe/name "Obst"
                              :ingredient/name "Kirschen"
                              :cooked-with/unit "Packung"}
                             {:recipe/name "Äpfel"
                              :ingredient/name "Obst"
                              :cooked-with/amount 3.0}
                             {:recipe/name "Bohnengulasch mit Räuchertofu"
                              :ingredient/name "Weiße Bohnen"
                              :cooked-with/unit "g"}
                             {:recipe/name "Milchreis"
                              :ingredient/name "Vanillezucker"
                              :cooked-with/unit "Packung"}
                             {:recipe/name "Flammkuchen vegetarisch"
                              :ingredient/name "Crème fraîche"
                              :cooked-with/unit "g"}
                             {:recipe/name "Vegetarische gefüllte Zucchini auf griechische Art"
                              :ingredient/name "Crème fraîche"
                              :cooked-with/unit "EL"}
                             {:recipe/name "Kartoffeln Lorraine"
                              :ingredient/name "Crème fraîche"
                              :cooked-with/unit "g"}
                             {:recipe/name "Pizza"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "g"}
                             {:recipe/name "Pide"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "g"}
                             {:recipe/name "Flammkuchen vegetarisch"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "g"}
                             {:recipe/name "Langos"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "g"}
                             {:recipe/name "Spinatknoedel"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "EL"}
                             {:recipe/name "Blumenkohl-Schnitzel"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "EL"}
                             {:recipe/name "Kartoffelpuffer"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "EL"}
                             {:recipe/name "Kohlrabi in Parmesan-Kräuter-Panade"
                              :ingredient/name "Mehl"
                              :cooked-with/unit "Tasse"}
                             {:recipe/name "Pasta mit Brokkoli"
                              :ingredient/name "Pinienkerne"
                              :cooked-with/unit "g"}
                             {:recipe/name "Langos"
                              :ingredient/name "Hefe"
                              :cooked-with/unit "Würfel"}
                             {:recipe/name "Pizza"
                              :ingredient/name "Hefe"
                              :cooked-with/unit "Würfel"}
                             {:recipe/name "Pide"
                              :ingredient/name "Hefe"
                              :cooked-with/unit "Würfel"}
                             {:recipe/name "Teigtaschen mit Spinat-Feta-Füllung"
                              :ingredient/name "Hefe"
                              :cooked-with/unit "g"}
                             {:recipe/name "Erbsen Fusilli in Spinatsoße"
                              :ingredient/name "Muskat"
                              :cooked-with/unit "EL"}
                             {:recipe/name "Brokkoli-Nudelauflauf"
                              :ingredient/name "Muskat"
                              :cooked-with/unit "Msp."}
                             {:recipe/name "Mandel Pasta mit Champigons"
                              :ingredient/name "Muskat"
                              :cooked-with/unit "TL"}
                             {:recipe/name "Vegetarisches Pastizio"
                              :ingredient/name "Mozzarella"
                              :cooked-with/unit "g"}
                             {:recipe/name "Cremiger Nudelauflauf"
                              :ingredient/name "Mozzarella"
                              :cooked-with/unit "g"}
                             {:recipe/name "Gefüllte Paprika mit Feta und Mozarella"
                              :ingredient/name "Mozzarella"
                              :cooked-with/unit "g"}
                             {:recipe/name "Teigtaschen mit Spinat-Feta-Füllung"
                              :ingredient/name "Minze"
                              :cooked-with/unit "g"}
                             {:recipe/name "Zucchini-Nudeln in cremiger Ricotta-Sauce"
                              :ingredient/name "Minze"
                              :cooked-with/unit "g"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Nudeln"
                              :cooked-with/amount-desc "150g"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Öl"
                              :cooked-with/amount-desc "2EL"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Karotte"
                              :cooked-with/amount-desc "1"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Zucchini"
                              :cooked-with/amount-desc "1/2"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Zitrone"
                              :cooked-with/amount-desc "1/2"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Chili"
                              :cooked-with/amount-desc "1/2"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Pfeffer"
                              :cooked-with/amount-desc "1 Prise"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Avocado"
                              :cooked-with/amount-desc "1"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Zwiebel"
                              :cooked-with/amount-desc "1"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Kräuter"
                              :cooked-with/amount-desc "eine Prise"}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Pfeffer"
                              :cooked-with/amount 1.0}
                             {:recipe/name "Vegetarisches Chili mit Bulgur"
                              :ingredient/name "Zimt"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Kartoffelcurry mit Erbsen und Tomaten"
                              :ingredient/name "Zimt"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Vegetarisches Pastizio"
                              :ingredient/name "Gemüsebrühe"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Vegetarisches Pastizio"
                              :ingredient/name "Passierte Tomaten"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Penne con verdura"
                              :ingredient/name "Zitrone"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Vegetarisches Chili mit Bulgur"
                              :ingredient/name "Bulgur"
                              :cooked-with/amount 0.75}
                             {:recipe/name "Curry"
                              :ingredient/name "Chinakohl"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Kartoffelcurry mit Erbsen und Tomaten"
                              :ingredient/name "Petersilie"
                              :cooked-with/amount 0.5}
                             {:recipe/name "Käse-Spätzle"
                              :ingredient/name "Petersilie"
                              :cooked-with/amount 0.5}])

(defn unit-from-amount-desc [conn {:keys [re-extractor unit]}]
  (->> (d/q '[:find
              (pull ?i
                    [:ingredient/name])
              (pull ?c
                    [:cooked-with/amount
                     :cooked-with/unit
                     :cooked-with/amount-desc])
              (pull ?r
                    [:recipe/name])
              :where
              [?i :ingredient/id]
              [?c :cooked-with/ingredient ?i]
              [?c :cooked-with/recipe ?r]]
            (d/db conn))
       (filter (fn [[_ {:keys [cooked-with/amount
                              cooked-with/amount-desc
                              cooked-with/unit]}]]
                 (and (or amount amount-desc unit)
                      (nil? unit)
                      (re-matches re-extractor amount-desc))))
       (map (fn [[{:keys [ingredient/name]} _ recipe]]
              {:recipe/name (:recipe/name recipe)
               :ingredient/name name
               :cooked-with/unit unit}))))

(def unit-extracter
  [{:re-extractor  #"[\d/½¼¾]+\s*g"
    :unit "g"}
   {:re-extractor  #"[\d/½¼¾]+\s*(EL|Esslöffel.*)"
    :unit "EL"}
   {:re-extractor  #"[\d/½¼¾]+\s*TL.*"
    :unit "TL"}
   {:re-extractor  #"[\d/½¼¾]+\s*kg"
    :unit "kg"}
   {:re-extractor  #"[\d/½¼¾]+\s*ml"
    :unit "ml"}
   {:re-extractor  #"[\d/½¼¾]+\s*(Stk|Stück).*"
    :unit "Stück"}
   {:re-extractor  #"[\d/½¼¾]+\s*Prise\(?n?\)?\s?"
    :unit "Prise"}
   {:re-extractor  #"[\d/½¼¾]+\s*Zehe\/?n?"
    :unit "Zehe"}
   {:re-extractor  #"[\d/½¼¾]+\s*Dose\/?n?"
    :unit "Dose"}
   {:re-extractor  #"[\d/½¼¾]+\s*Tasse\/?n?"
    :unit "Tase"}
   {:re-extractor  #"[\d/½¼¾]+\s*Bund"
    :unit "Bund"}
   {:re-extractor  #"[\d/½¼¾]+\s*Packunge?n?"
    :unit "Tase"}])

(defn extract-units [conn]
  (mapcat (partial unit-from-amount-desc conn) unit-extracter))

(defn migrate-ingredient-amounts [conn]
  (db/transact
   conn
   (map #(update-unit conn %) (concat cooked-with-migrations (extract-units conn)))))
