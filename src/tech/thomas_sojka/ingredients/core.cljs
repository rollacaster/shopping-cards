(ns tech.thomas-sojka.ingredients.core
  (:require [cljs.reader :refer [read-string]]
            [clojure.string :as s]
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

(def ingredients '({:shopping [{:amount-desc "4 EL", :name "Joghurt", :amount 4}],
  :category "Käse & Co",
  :name "Joghurt"}
 {:shopping [{:amount-desc "100 g", :name "Parmesan", :amount 100}
             {:amount-desc nil, :name "Parmesan", :amount nil}],
  :category "Käse & Co",
  :name "Parmesan"}
 {:shopping [{:amount-desc "1 Pck.", :name "Hefe", :amount 1}],
  :category "Käse & Co",
  :name "Hefe"}
 {:shopping [{:amount-desc "800 g", :name "Spinat", :amount 800}
             {:amount 750, :name "Spinat", :amount-desc "750"}
             {:amount-desc "800 g", :name "Spinat", :amount 800}],
  :category "Tiefkühl",
  :name "Spinat"}
 {:shopping [{:amount-desc "1", :name "Eier", :amount 1}
             {:amount-desc "2", :name "Eier", :amount 2}],
  :category "Eier",
  :name "Eier"}
 {:shopping [{:amount-desc "2 m.-große", :name "Zwiebel", :amount 2}
             {:amount 2, :name "Zwiebel", :amount-desc "2"}
             {:amount-desc "1 kleine", :name "Zwiebel", :amount 1}],
  :category "Gemüse",
  :name "Zwiebel"}
 {:shopping [{:amount-desc "2", :name "Knoblauch", :amount 2}
             {:amount-desc "1 Zehe/n", :name "Knoblauch", :amount 1}],
  :category "Gemüse",
  :name "Knoblauch"}
 {:shopping [{:amount-desc "400 g", :name "Geriebener Käse", :amount 400}
             {:amount nil, :name "Geriebener Käse", :amount-desc nil}],
  :category "Käse & Co",
  :name "Geriebener Käse"}
 {:shopping [{:amount 1, :name "Reis", :amount-desc "1"}],
  :category "Beilage",
  :name "Reis"}
 {:shopping [{:amount-desc "250 ml", :name "Milch", :amount 250}],
  :category "Käse & Co",
  :name "Milch"}
 {:shopping [{:amount-desc "300 g", :name "Weißbrot", :amount 300}],
  :category "Brot & Co",
  :name "Weißbrot"}
 {:shopping [{:amount-desc "20 Scheibe/n", :name "Salami", :amount 20}],
  :category "Wursttheke",
  :name "Salami"}
 {:shopping [{:amount-desc "30 g", :name "Butter", :amount 30}
             {:amount-desc "80 g", :name "Butter", :amount 80}],
  :category "Käse & Co",
  :name "Butter"}
 {:shopping [{:amount-desc "350 g", :name "Frischkäse", :amount 350}],
  :category "Käse & Co",
  :name "Frischkäse"}
 {:shopping [{:amount nil, :name "Blumenkohl", :amount-desc nil}],
  :category "Gemüse",
  :name "Blumenkohl"}
 {:shopping [{:amount-desc "400 g", :name "Passierte Tomaten", :amount 400}],
  :category "Konserven",
  :name "Passierte Tomaten"}
 {:shopping [{:amount nil, :name "Zitrone", :amount-desc nil}],
  :category "Gemüse",
  :name "Zitrone"}
 {:shopping [{:amount-desc "250 ml", :name "Wasser", :amount 250}],
  :category "Getränke",
  :name "Wasser"}
 {:shopping [{:amount-desc nil, :name "Lasagneplatten", :amount nil}],
  :category "Beilage",
  :name "Lasagneplatten"}))

(defn ingredient [{:keys [i name shopping selected? on-change]}]
  [:li.flex.items-center.ph4.pv3 {:class (if (= (mod i 2) 0) "bg-light-gray near-black" "bg-gray white")}
   [:input.pointer.mh2
    {:id name :type "checkbox" :checked selected? :on-change on-change}]
   [:label.pointer.f4
    {:for name}
    (str (count (map :amount shopping))
         " " name
         " (" (s/join ", " (map :amount-desc shopping)) ")")]])

(defn app []
  (let [step (r/atom "SELECT_RECIPE")
        selected-recipes (r/atom #{"Pide"})
        selected-ingredients (r/atom (set (map :name ingredients)))
        recipes (r/atom [])]
    (-> (.fetch js/window "/recipes.edn")
        (.then #(.text %))
        (.then read-string)
        (.then #(reset! recipes %)))
    (fn []
      [:div.sans-serif
       [:header.bg-dark-gray.white.pv3.ph4
        [:div.ml2 [:h1.ma0
                   (case @step
                     "SELECT_RECIPE" "Select Recipes"
                     "DESELECT_INGREDIENTS" "Remove available ingredients"
                     "TODO")]]]
       [:main
        (case @step
          "SELECT_RECIPE"
          [:div.flex.flex-wrap.justify-center.justify-start-ns.ph4.pb6.mt3
           (doall
            (map (fn [{:keys [name link image]}]
                   [recipe (let [selected? (contains? @selected-recipes name)]
                             {:key name
                              :name name
                              :link link
                              :image image
                              :selected? selected?
                              :on-click #(swap! selected-recipes
                                                (fn [selected-recipes]
                                                  ((if selected? disj conj)
                                                   selected-recipes name)))})])
                 @recipes))]
          "DESELECT_INGREDIENTS"
          [:ul.list.pl0.mv0
           (doall
            (map-indexed (fn [i {:keys [name shopping]}]
                           [ingredient
                            (let [selected?
                                  (contains? @selected-ingredients name)]
                              {:key name
                               :i i
                               :name name
                               :selected? selected?
                               :shopping shopping
                               :on-change
                               #(swap! selected-ingredients
                                       (fn [selected-ingredients]
                                         ((if selected? disj conj)
                                          selected-ingredients name)))})])
                         (->> ingredients
                              (group-by :category)
                              (map second)
                              flatten)))]
          "TODO")]
       (when (and (> (count @selected-recipes) 0))
         [:footer.fixed.bottom-0.w-100.bg-dark-gray.flex.justify-center.pa3
          [:button.br2.bg-light-gray.pointer
           {:on-click (fn []
                        (case @step
                          "SELECT_RECIPE"
                          (reset! step "DESELECT_INGREDIENTS")
                          "DESELECT_INGREDIENTS"
                          (prn (filter
                                #(contains? @selected-ingredients (:name %))
                                ingredients))))}
           [:div.flex.items-center
            [:span.f2.mr2 "Fertig"]
            [:span.w2.h2 [icon :check-mark]]]]])])))


(dom/render [app] (.getElementById js/document "app"))
