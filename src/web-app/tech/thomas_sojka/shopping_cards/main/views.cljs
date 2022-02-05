(ns tech.thomas-sojka.shopping-cards.main.views
  (:require [re-frame.core :refer [dispatch subscribe]]
            ["date-fns" :refer (addDays startOfDay format subDays addDays isPast)]
            ["date-fns/locale" :refer (de)]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.main.components :as c]))

(defn meal-name [meal-plan]
  (if (:recipe meal-plan)
    (:name (:recipe meal-plan))
    (case (:type meal-plan)
      :meal-type/lunch "Mittagessen"
      :meal-type/dinner "Abendessen")))

(defn meal [meal-plan]
  (let [has-recipe? (:recipe meal-plan)]
    [:button.pt2.ph2.h-50.bg-transparent.bn.w-100.relative
     {:on-click #(dispatch
                  (if has-recipe?
                    [:recipe-details/show-meal-details meal-plan]
                    [:main/select-meal meal-plan]))}
     [:div.h-100.br3.bg-center.cover.relative
      {:style {:background-image (if has-recipe? (str "url(" (:image (:recipe meal-plan)) ")") "")}}
      (when has-recipe? [:div.o-40.bg-orange.absolute.h-100.w-100.br3])
      [:h4.f4.fw5.mv0.br3.h-100.bw1.overflow-hidden.flex.justify-center.items-center.absolute.w-100
       {:class (r/class-names (if has-recipe? "white" "ba b--gray b--dashed gray"))}
       (when (:shopping-list meal-plan)
         [:div.absolute.bottom-0.right-0.mr1.bg-orange-400.br-100
          {:style {:width "1.8rem" :padding 5}}
          [c/icon :shopping-cart]])
       (meal-name meal-plan)]]]))

(defn meal-plan []
  (dispatch [:main/load-recipes])
  (dispatch [:main/init-meal-plans (js/Date.)])
  (fn []
    (let [meals-plans @(subscribe [:main/weekly-meal-plans])
          start-of-week @(subscribe [:main/start-of-week])
          meals-without-shopping-list @(subscribe [:main/meals-without-shopping-list])]
      [:div.ph5-ns.flex.flex-column.h-100
       [:div.flex.items-center.justify-between
        [:div.pv2.flex
         [:button.pv2.w3.bg-gray-600.ba.br3.br--left.white.b--white.tc.flex.justify-center
          {:on-click
           #(dispatch [:main/init-meal-plans (startOfDay (js/Date.))])}
          "Heute"]
         [:button.pv2.w3.bg-gray-600.ba.bl-0.br-0.white.b--white.tc.flex.justify-center
          {:on-click
           #(dispatch [:main/init-meal-plans (subDays (startOfDay start-of-week) 4)])}
          "Zurück"]
         [:button.pv2.w3.bg-gray-600.ba.br3.br--right.white.b--white.tc.flex.justify-center
          {:on-click
           #(dispatch [:main/init-meal-plans (addDays (startOfDay start-of-week) 4)])}
          "Vor"]]
        [:div.flex.justify-center.flex-auto
         (format (:date (ffirst meals-plans)) "MMMM yyyy" #js {:locale de})]]
       [:div.flex.flex-wrap.flex-auto
        (doall
         (map
          (fn [[lunch dinner]]
            (let [bank-holiday @(subscribe [:main/bank-holiday (:date lunch)])]
              ^{:key (:date lunch)}
              [:div.ba.w-50.pv2.flex.flex-column.b--gray.h-50
               [:div.ph2.flex.justify-between
                [:span.truncate.dark-red bank-holiday]
                [:span.tr.fw6
                 {:style {:white-space "nowrap"}}
                 (format (:date lunch) "EEEEEE dd.MM" #js {:locale de})]]
               [:div.flex-auto
                {:class (when (isPast (addDays (startOfDay start-of-week) 2)) "o-20")}
                [meal lunch]
                [meal dinner]]]))
          meals-plans))]
       (when (seq meals-without-shopping-list)
         [c/footer {:on-click #(dispatch [:shopping-card/load-ingredients-for-meals meals-without-shopping-list])}])])))
