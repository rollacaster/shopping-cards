(ns tech.thomas-sojka.shopping-cards.core-test
  (:require
   [tech.thomas-sojka.shopping-cards.testing-library :refer [wait-for get-all-by-role get-by-role]]
   [cljs-bean.core :refer [bean]]
   [cljs.test :as t :include-macros true]
   [clojure.string :as str]
   [promesa.core :as p]
   [tech.thomas-sojka.browser :as browser-mocks]
   [tech.thomas-sojka.shopping-cards.core :as sut]))

(def screen (js/document.createElement "div"))

(defn setup-app []
  (set! (.-href js/location) "#")
  (set! (.-id screen) "app")
  (set! (.-className screen) "h-100")
  (js/document.body.appendChild screen)
  (-> (.start browser-mocks/worker)
      (.then (fn []
               (sut/init! {:service-worker false
                           :container screen})))))

(defn teardown-app []
  (.remove screen)
  (.stop browser-mocks/worker)
  (set! (.-href js/location) "#"))

(t/use-fixtures :each
  {:before setup-app
   :after teardown-app})
(comment
  (setup-app))
(t/deftest create-shopping-card []
  (t/async done
           (p/do
             (wait-for #(get-all-by-role screen "button" {:name "Mittagessen"}) #js {:timeout 10000})
             (.click (first (get-all-by-role screen "button" {:name "Mittagessen"})))

             (wait-for #(get-by-role screen "button" {:name "Soup"}))
             (.click (get-by-role screen "button" {:name "Soup"}))

             (wait-for #(get-by-role screen "button" {:name "Fertig!"}))
             (.click (get-by-role screen "button" {:name "Fertig!"}))

             (wait-for #(get-by-role screen "checkbox" {:name "1 Carrot"}))
             (.click (get-by-role screen "checkbox" {:name "1 Carrot"}))

             (wait-for #(get-by-role screen "button" {:name "Fertig!"}))
             (.click (get-by-role screen "button" {:name "Fertig!"}))

             (wait-for #(get-by-role screen "link" {:name "In Trello anzeigen"}))
             (t/is (str/includes? (:hash (bean js/document.location)) "fake-trello-card-id"))
             (done))))
