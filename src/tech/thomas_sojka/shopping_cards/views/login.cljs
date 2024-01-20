(ns tech.thomas-sojka.shopping-cards.views.login
  (:require ["firebase/auth" :as auth]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.auth :refer [auth]]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(defonce err (r/atom nil))

(defn main []
  [:form.pa4
   {:on-submit (fn [e]
                 (.preventDefault e)
                 (-> (let [{:strs [email password]} (apply hash-map (mapcat identity (.entries (new js/FormData ^js (.-target e)))))]
                       (auth/signInWithEmailAndPassword auth email password))
                     (.then (fn []
                              (reset! err nil)
                              (dispatch [:auth/logged-in])))
                     (.catch (fn [error]
                               (prn (.-code error))
                               (reset! err
                                       (case (.-code error)
                                         ("auth/user-not-found" "auth/wrong-password" "auth/internal-error")
                                         "Ungültie Email-Adresse oder Password"))))))}
   [c/input-box
    [c/label {:for "email"} "Email"]
    [c/input
     {:type "email"
      :name "email"
      :autoComplete "username"
      :required true}]]
   [c/input-box
    [c/label {:for "password"} "Passwort"]
    [c/input
     {:type "password" :name
      "password"
      :autoComplete "current-password"}]]
   [:div.flex.items-center
    [c/button
     "Login"]
    [:span.db.red.ml3 @err]]])
