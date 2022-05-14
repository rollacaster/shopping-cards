(ns tech.thomas-sojka.shopping-cards.core-test
  (:require [tech.thomas-sojka.shopping-cards.core :as sut]
            [cljs.test :as t :include-macros true]
            ["@testing-library/dom" :as tld]))

(defn setup-app []
  (let [tachyons-link-node (js/document.createElement "link")
        app-node (js/document.createElement "div")]
    (set! (.-href js/location) "#")
    (set! (.-rel tachyons-link-node) "stylesheet")
    (set! (.-href tachyons-link-node) "https://unpkg.com/tachyons@4.12.0/css/tachyons.min.css")
    (set! (.-id app-node) "app")
    (js/document.head.appendChild tachyons-link-node)
    (js/document.body.appendChild app-node)
    (sut/init!)))

(defn teardown-app []
  (.remove (js/document.getElementById "app"))
  (set! (.-href js/location) "#"))

(t/use-fixtures :once
  {:before setup-app
   :after teardown-app})

(t/deftest load-app []
  (t/async done
           (let [container (js/document.getElementById "app")]
             (.click (first (tld/getAllByText container "Mittagessen")))
             (-> (tld/waitFor (fn []
                                (tld/getByText container "Mittag auswählen")))
                 (.then (fn []
                          (t/is (tld/getByText container "Mittag auswählen"))
                          (done)))))))
