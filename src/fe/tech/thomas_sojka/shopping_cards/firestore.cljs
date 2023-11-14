(ns tech.thomas-sojka.shopping-cards.firestore
  (:require ["firebase/firestore" :as firestore]
            [cljs-bean.core :refer [->js ->clj]]
            [re-frame.core :refer [dispatch reg-fx]]
            [tech.thomas-sojka.shopping-cards.firebase :as firebase]))

(def db (.getFirestore firestore firebase/app))

(defn get-doc [path id]
  (-> (firestore/getDoc (firestore/doc db path id))
      (.then (fn [snap] (->clj (.data snap))))))

(defn delete-doc [path id]
  (firestore/deleteDoc (firestore/doc db path id)))

(reg-fx :firestore/add-docs
  (fn [{:keys [path data id on-success on-failure]}]
    (doseq [d data]
      (-> (firestore/setDoc (firestore/doc (firestore/collection db path) (id d))
                            (->js d))
          (.then (fn [] (when on-success (dispatch (conj on-success d)))))
          (.catch (fn [err] (when on-failure (dispatch (conj on-failure err)))))))))

(reg-fx :firestore/add-doc
  (fn [{:keys [path data on-success on-failure]}]
    (-> (firestore/setDoc (firestore/doc (firestore/collection db path) (:id data))
                          (->js data))
        (.then (fn [] (when on-success (dispatch (conj on-success data)))))
        (.catch (fn [err] (when on-failure (dispatch (conj on-failure err))))))))

(defn update-doc [data path segment]
  (firestore/setDoc (firestore/doc (firestore/collection db path) segment)
                    (->js data)))

(reg-fx :firestore/update-doc
  (fn [{:keys [path key data on-success on-failure]}]
    (-> (update-doc data path key)
        (.then (fn [] (when on-success (dispatch on-success))))
        (.catch (fn [err] (when on-failure (dispatch (conj on-failure err))))))))

(reg-fx :firestore/update-docs
  (fn [{:keys [path data id on-success on-failure]}]
    (doseq [d data]
      (-> (firestore/updateDoc (firestore/doc db path (id d)) (->js d))
          (.then (fn [] (when on-success (dispatch on-success))))
          (.catch (fn [err] (when on-failure (dispatch (conj on-failure err)))))))))

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
