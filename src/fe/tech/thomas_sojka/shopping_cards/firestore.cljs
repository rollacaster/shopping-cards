(ns tech.thomas-sojka.shopping-cards.firestore
  (:require ["firebase/firestore" :as firestore]
            [datascript.core :as d]
            [re-frame.core :refer [dispatch reg-fx]]
            [tech.thomas-sojka.shopping-cards.db :as db]
            [tech.thomas-sojka.shopping-cards.firebase :as firebase]))

(def db (.getFirestore firestore firebase/app))

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
