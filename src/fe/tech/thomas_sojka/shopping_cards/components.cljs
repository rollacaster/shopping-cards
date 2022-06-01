(ns tech.thomas-sojka.shopping-cards.components
  (:require [tech.thomas-sojka.shopping-cards.icons :as icons]))

(defn icon
  ([name]
   [icon {} name])
  ([{:keys [class style]} name]
   [:svg {:viewBox "0 0 24 24" :class class :style style}
    [:path {:d (icons/svg-paths name) :fill "currentColor"}]]))
