(ns tech.thomas-sojka.shopping-cards.auth
  (:require ["firebase/auth" :as auth]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.firebase :as firebase]))

(def auth (auth/getAuth firebase/app))

(defonce user (r/atom :loading))
(defonce user-sync
  (auth/onAuthStateChanged auth
                           (fn [firebase-user]
                             (reset! user
                                     (if firebase-user
                                       {:displayName (.-displayName firebase-user)
                                        :uid (.-uid firebase-user)
                                        :email (.-email firebase-user)
                                        :photoURL (.-photoURL firebase-user)}
                                       :noauth)))))

(comment
  (auth/signInWithEmailAndPassword auth "thsojka@web.de" "test123")
  (auth/signOut auth))
