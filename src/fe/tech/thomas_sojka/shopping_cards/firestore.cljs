(ns tech.thomas-sojka.shopping-cards.firestore
  (:require ["firebase/firestore" :as firestore]
            [datascript.core :as d]
            [re-frame.core :refer [dispatch reg-fx]]
            [tech.thomas-sojka.shopping-cards.db :as db]
            [tech.thomas-sojka.shopping-cards.firebase :as firebase]))

(def db (.getFirestore firestore firebase/app))

(defn add-recipe [recipe]
  (firestore/setDoc (firestore/doc (firestore/collection db "recipes") (:recipe/id recipe))
                    (clj->js recipe)))

(defn add-ingredient [ingredient]
  (firestore/setDoc (firestore/doc (firestore/collection db "ingredients") (:ingredient/id ingredient))
                    (clj->js ingredient)))

(defn log-recipes []
  (-> (firestore/getDocs (firestore/collection db "recipes"))
      (.then (fn [snapshot]
               (let [datoms-snapshot (volatile! [])]
                 (.forEach snapshot
                           (fn [doc]
                             (vswap! datoms-snapshot conj (-> doc
                                                              .data
                                                              (js->clj :keywordize-keys true)))))
                 (js/console.log @datoms-snapshot) )))))

(defn migrate-recipes []
  (doseq [[recipe] (d/q
                    '[:find (pull ?r
                                  [[:recipe/id]
                                   [:recipe/name]
                                   [:recipe/image]
                                   [:recipe/link]
                                   [:recipe/type]
                                   {[:cooked-with/_recipe :as :ingredients]
                                    [[:cooked-with/amount]
                                     [:cooked-with/unit]
                                     [:cooked-with/amount-desc]
                                     {[:cooked-with/ingredient]
                                      [[:ingredient/name]
                                       [:ingredient/id]
                                       [:ingredient/category]]}]}])
                      :where [?r :recipe/id]]
                    @db/conn)]
    (add-recipe recipe)))

(defn migrate-ingredients []
  (doseq [[ingredient] (d/q
                        '[:find (pull ?i
                                      [[:ingredient/name]
                                       [:ingredient/id]
                                       [:ingredient/category]])
                          :where [?i :ingredient/id]]
                        @db/conn)]
    (add-ingredient ingredient)))


(reg-fx :firestore/doc
  (fn [{:keys [path key on-success on-failure]}]
    (-> (firestore/getDoc (firestore/doc db path key))
        (.then (fn [doc-snap] (dispatch (conj on-success (.data doc-snap)))))
        (.catch (fn [err] (dispatch (conj on-failure err)))))))

(comment
  (firestore/onSnapshot
   (firestore/query (firestore/collection db "datoms"))
   (fn [snapshot]
     (let [datoms-snapshot (volatile! [])]
       (.forEach snapshot
                 (fn [doc]
                   (vswap! datoms-snapshot conj (-> doc
                                                     .data
                                                     (js->clj :keywordize-keys true)))))
       (prn "hello" datoms-snapshot))))
  (def snapshots-ref (firestore/collection db "snapshots"))
  (firestore/setDoc (firestore/doc snapshots-ref "2023-01-24")
                    #js {:content (js/JSON.stringify (d/serializable @db/conn))}))
