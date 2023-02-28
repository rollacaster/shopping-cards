(ns tech.thomas-sojka.shopping-cards.auth
  (:require ["firebase/auth" :as auth]
            [re-frame.core :refer [dispatch dispatch-sync reg-event-fx]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.firebase :as firebase]))

(def auth (auth/getAuth firebase/app))

(defonce user (r/atom :loading))
(defonce user-sync
  (auth/onAuthStateChanged auth
                           (fn [firebase-user]
                             (if firebase-user
                               (do
                                 (dispatch-sync [:app/load (js/Date.)])
                                 (reset! user
                                         {:displayName (.-displayName firebase-user)
                                          :uid (.-uid firebase-user)
                                          :email (.-email firebase-user)
                                          :photoURL (.-photoURL firebase-user)}))
                               (do
                                 (dispatch [:auth/logged-out])
                                 (reset! user :noauth))))))

(reg-event-fx :auth/logged-out
  (fn []
    {:app/push-state [:route/login]}))

(reg-event-fx :auth/logged-in
  (fn []
    {:app/push-state [:route/main]}))

(comment
  (auth/signInWithEmailAndPassword auth "thsojka@web.de" "test123")

  (auth/signOut auth))
