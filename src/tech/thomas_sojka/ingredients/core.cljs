(ns tech.thomas-sojka.ingredients.core
  (:require [cljs.reader :refer [read-string]]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"})

(defn icon [name]
  [:svg {:viewBox "0 0 24 24"}
   [:path {:d (icons name)}]])

(defn recipe [{:keys [name image selected? on-click]}]
  [:button.bn.bg-transparent.outline-transparent {:on-click on-click}
   [:div.w5.h5.relative.ma2.shadow-3 {:class (when selected? "o-50")}
    (when selected?
      [:div.w4.h4.absolute {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
       [icon :check-mark]])
    [:img.br2.w-100.h-100 {:style {:object-fit "cover"} :src image}]
    [:div.bg-dark-gray.absolute.pa2.mh2.mb2.bottom-0.o-50.br2
     [:span.white.f4 name]]
    [:div.absolute.pa2.mh2.mb2.bottom-0
     [:span.white.f4 name]]]])

(defn app []
  (let [recipes (r/atom [])
        selected-recipes (r/atom #{"Pide"})]
    (fn []
      (-> (.fetch js/window "/recipes.edn")
          (.then #(.text %))
          (.then read-string)
          (.then #(reset! recipes %)))
      [:div.sans-serif
       [:header.bg-dark-gray.mb3.white.pv3.ph4
        [:div.ml2 [:h1.ma0 "Select Recipes"]]]
       [:main.ph4.pb6
        [:div.flex.flex-wrap.justify-center.justify-start-ns
         (doall
          (map (fn [{:keys [name link image]}]
                 [recipe (let [selected? (contains? @selected-recipes name)]
                           {:key name :name name :link link :image image :selected? selected?
                            :on-click #(swap! selected-recipes (fn [selected-recipes] ((if selected? disj conj) selected-recipes name)))})])
               @recipes))]]
       (when (> (count @selected-recipes) 0)
         [:footer.fixed.bottom-0.w-100.bg-dark-gray.flex.justify-center.pa3
          [:button.br2.bg-light-gray.pointer
           {:on-click #(println @selected-recipes)}
           [:div.flex.items-center [:span.f2.mr2 "Fertig"] [:span.w2.h2 [icon :check-mark]]]]])])))



(dom/render [app] (.getElementById js/document "app"))

