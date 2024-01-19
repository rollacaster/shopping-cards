(ns tech.thomas-sojka.shopping-cards.core-test
  (:require [cljs.test :as t :include-macros true]
            [promesa.core :as p]
            [tech.thomas-sojka.shopping-cards.core :as sut]
            [tech.thomas-sojka.shopping-cards.mocks :as mocks]
            [tech.thomas-sojka.shopping-cards.testing-library :refer [get-all-by-role
                                                                      get-by-role wait-for]]))

(def screen (js/document.createElement "div"))

(defn setup-app []
  (set! (.-href js/location) "#")
  (set! (.-id screen) "app")
  (set! (.-className screen) "h-100")
  (js/document.body.appendChild screen)
  (sut/init! {:container screen}))

(defn teardown-app []
  (.remove screen)
  (set! (.-href js/location) "#"))

(t/use-fixtures :each
  {:before setup-app
   :after teardown-app})
(comment
  (setup-app))

(t/deftest create-shopping-card []
  (mocks/overwrite-firestore)
  (t/async done
           (p/do
             (wait-for #(get-all-by-role screen "link" {:name "Mittagessen"}) #js {:timeout 10000})
             (.click (first (get-all-by-role screen "link" {:name "Mittagessen"})))

             (wait-for #(get-by-role screen "button" {:name "Soup"}))
             (.click (get-by-role screen "button" {:name "Soup"}))

             (wait-for #(get-by-role screen "button" {:name "Fertig!"}))
             (.click (get-by-role screen "button" {:name "Fertig!"}))

             (wait-for #(get-by-role screen "checkbox" {:name "1 Carrot"}))
             (.click (get-by-role screen "checkbox" {:name "1 Carrot"}))

             (wait-for #(get-by-role screen "button" {:name "Fertig!"}))
             (.click (get-by-role screen "button" {:name "Fertig!"}))

             (wait-for #(get-by-role screen "heading" {:name "Einkaufsliste"}))
             (get-by-role screen "checkbox" {:name "1 Mushroom"})

             (done))))
