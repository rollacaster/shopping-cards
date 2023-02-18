(ns tech.thomas-sojka.shopping-cards.firestore
  (:require ["firebase/firestore" :as firestore]
            [re-frame.core :refer [dispatch reg-fx]]
            [tech.thomas-sojka.shopping-cards.firebase :as firebase]))

(def db (.getFirestore firestore firebase/app))

(reg-fx :firestore/doc
  (fn [{:keys [path key on-success on-failure]}]
    (-> (firestore/getDoc (firestore/doc db path key))
        (.then (fn [doc-snap] (dispatch (conj on-success (.data doc-snap)))))
        (.catch (fn [err] (dispatch (conj on-failure err)))))))

(reg-fx :firestore/add-docs
  (fn [{:keys [path data on-success on-failure]}]
    (doseq [{:keys [id] :as d} data]
      (-> (firestore/setDoc (firestore/doc (firestore/collection db path) id)
                            (clj->js d))
          (.then (fn [] (when on-success (dispatch (conj on-success data)))))
          (.catch (fn [err] (when on-failure (dispatch (conj on-failure err)))))))))

(reg-fx :firestore/add-doc
  (fn [{:keys [path data on-success on-failure]}]
    (-> (firestore/setDoc (firestore/doc (firestore/collection db path) (:id data))
                          (clj->js data))
        (.then (fn [] (when on-success (dispatch (conj on-success data)))))
        (.catch (fn [err] (when on-failure (dispatch (conj on-failure err))))))))

(reg-fx :firestore/remove-doc
  (fn [{:keys [path key on-success on-failure]}]
    (-> (firestore/deleteDoc (firestore/doc db path key))
        (.then (fn [] (when on-success (dispatch (conj on-success)))))
        (.catch (fn [err] (when on-failure (dispatch (conj on-failure err))))))))

(reg-fx :firestore/snapshot
  (fn [{:keys [path on-success on-failure]}]
    (firestore/onSnapshot
     (firestore/query (firestore/collection db path))
     (fn [snapshot]
       (let [data (volatile! [])]
         (.forEach snapshot (fn [doc] (vswap! data conj (-> doc .data (js->clj :keywordize-keys true)))))
         (dispatch (conj on-success @data))))
     (fn [error]
       (dispatch (conj on-failure error))))))
