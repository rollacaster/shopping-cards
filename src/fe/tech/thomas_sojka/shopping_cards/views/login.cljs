(ns tech.thomas-sojka.shopping-cards.views.login
  (:require ["firebase/auth" :as auth]
            [re-frame.core :refer [dispatch]]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.auth :refer [auth]]))

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
   [:div.pb4
    [:label.db.mb3 {:for "email"} "Email"]
    [:input.w-100.pv3.bn.br1.shadow-1
     {:type "email" :name "email" :autoComplete "username" :required true}]]
   [:div.pb4
    [:label.db.mb3 {:for "password"} "Passwort"]
    [:input.w-100.pv3.bn.br1.shadow-1
     {:type "password" :name "password" :autoComplete "current-password"}]]
   [:div.flex.items-center
    [:button.bg-orange-400.bn.ph5.pv3.br2.shadow-5.fw6
     "Login"]
    [:span.db.red.ml3 @err]]])
