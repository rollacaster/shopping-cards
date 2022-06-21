(ns tech.thomas-sojka.shopping-cards.core-test
    (:require [tech.thomas-sojka.shopping-cards.core :as sut]
              [cljs.test :as t :include-macros true]
              ["@testing-library/dom" :as tld]
              [tech.thomas-sojka.browser :as browser-mocks]))

(def tachyons-link-node (js/document.createElement "link"))
(def style-link-node (js/document.createElement "link"))
(def container (js/document.createElement "div"))
(defn setup-app []
  (-> (.start browser-mocks/worker)
      (.then (fn []
               (set! (.-href js/location) "#")
               (set! (.-rel tachyons-link-node) "stylesheet")
               (set! (.-href tachyons-link-node) "https://unpkg.com/tachyons@4.12.0/css/tachyons.min.css")
               (set! (.-rel style-link-node) "stylesheet")
               (set! (.-href style-link-node) "./styles.css")
               (set! (.-id container) "app")
               (js/document.head.appendChild tachyons-link-node)
               (js/document.head.appendChild style-link-node)
               (js/document.body.appendChild container)
               (sut/init! {:container container})))))

(defn teardown-app []
  (.remove container)
  (.remove tachyons-link-node)
  (.remove style-link-node)
  (.stop browser-mocks/worker)
  (set! (.-href js/location) "#"))

(t/use-fixtures :each
  {:before setup-app
   :after teardown-app})

(t/deftest load-app []
  (t/async done
           (-> (tld/waitFor #(tld/getAllByText container "Mittagessen"))
               (.then #(.click (first (tld/getAllByText container "Mittagessen"))))
               (.then #(tld/waitFor (fn [] (tld/getByText container "Mittag auswählen"))))
               (.then (fn []
                        (t/is (tld/getByText container "Mittag auswählen"))
                        (done))))))
