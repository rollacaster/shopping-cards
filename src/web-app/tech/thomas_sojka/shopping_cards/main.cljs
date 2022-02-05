(ns tech.thomas-sojka.shopping-cards.main
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-sub dispatch subscribe]]
            [ajax.core :as ajax]
            ["date-fns" :refer (addDays startOfDay isAfter getDate getMonth format subDays addDays isPast)]
            [cljs.reader :refer [read-string]]
            ["date-fns/locale" :refer (de)]
            [reagent.core :as r]
            [tech.thomas-sojka.shopping-cards.components :as c]))

(reg-event-fx
 :main/load-recipes
 (fn [{:keys [db]}]
   (if (empty? (:main/recipes db))
     {:db (assoc db :app/loading true)
      :http-xhrio {:method :get
                   :uri "recipes"
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:main/success-recipes]
                   :on-failure [:main/failure-recipes]}}
     {:db db})))

(reg-event-db
 :main/success-recipes
 (fn [db [_ data]]
   (-> db
       (assoc :app/loading false)
       (assoc :main/recipes data))))

(reg-event-db
 :main/failure-recipes
 (fn [db _]
   (-> db
       (assoc :app/loading false)
       (assoc :main/recipes :ERROR))))


(defn meal-plans-loaded-for-month? [db month-idx]
  (->> (:main/meal-plans db)
       (map #(.getMonth (:date %)))
       (some #(= month-idx %))))

(reg-event-fx
 :main/init-meal-plans
 (fn [{:keys [db]} [_ start-of-week]]
   ;; TODO Load meals for all visible days
   (if (meal-plans-loaded-for-month? db (.getMonth start-of-week))
     {:db (assoc db :main/start-of-week start-of-week)}
     {:db (-> db
              (assoc :app/loading true)
              (assoc :main/start-of-week start-of-week))
      :http-xhrio {:method :get
                   :uri (str "/meal-plans/" (inc (.getMonth start-of-week)))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:main/success-meal-plans]
                   :on-failure [:main/failure-meal-plans]}})))

(reg-event-db
 :main/success-meal-plans
 (fn [db [_ data]]
   (-> db
       (assoc :app/loading false)
       (update :main/meal-plans
               concat
               (map (fn [meal-plan]
                      (-> meal-plan
                          (update :type keyword)
                          (update :date #(js/Date. %))))
                    data)))))

(reg-event-db
 :main/failure-meal-plans
 (fn [db _]
   (-> db
       (assoc :app/loading false)
       (assoc :main/meal-plans :ERROR))))

(reg-event-fx
 :main/add-meal
 (fn [{:keys [db]} [_ recipe]]
   (prn :main/add-meal recipe)
   {:db (-> db
            (update :main/meal-plans conj (assoc (:recipe-details/meal db) :recipe recipe))
            (assoc :recipe-details/meal nil))
    :app/push-state [:route/main]
    :http-xhrio {:method :post
                 :uri "/meal-plans"
                 :params (assoc (:recipe-details/meal db) :recipe recipe)
                 :format (ajax/json-request-format)
                 :response-format (ajax/text-response-format)
                 :on-failure [:main/failure-add-meal (:recipe-details/meal db)]}}))

(reg-event-db
 :main/success-bank-holidays
 (fn [db [_ data]]
   (assoc db :main/bank-holidays (read-string data))))

(reg-event-db
 :main/failure-bank-holidays
 (fn [db _]
   ;; TODO Handle failed bank holiday loading
   db))

(defn remove-meal [meal-plans {:keys [date type]}]
  (remove (fn [m] (and (= date (:date m))
                      (= type (:type m))))
          meal-plans))

(reg-event-fx
 :main/failure-add-meal
 (fn [{:keys [db]} [_ failed-meal]]
   {:db (-> db
            (update :main/meal-plans remove-meal failed-meal)
            (assoc :app/error "Fehler: Speichern fehlgeschlagen"))
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-event-fx
 :main/select-meal
 (fn [{:keys [db]} [_ meal]]
   {:app/push-state
    (if (= (:type meal) :meal-type/lunch)
      [:tech.thomas-sojka.shopping-cards.view/select-lunch]
      [:tech.thomas-sojka.shopping-cards.view/select-dinner])
    :db (assoc db :recipe-details/meal meal)}))

(reg-event-fx
 :main/restart
 (fn [{:keys [db]} _]
   {:app/push-state [:route/main]
    :db (assoc db :shopping-card/selected-ingredient-ids #{})}))

(reg-event-fx
 :main/remove-meal
 (fn [{:keys [db]}]
   (let [{:keys [date type]} (:recipe-details/meal db)]
     {:db
      (update db :main/meal-plans remove-meal (:recipe-details/meal db))
      :app/push-state [:route/meal-plan]
      :http-xhrio {:method :delete
                   :uri "/meal-plans"
                   :url-params {:date (.toISOString date) :type type}
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-failure [:main/failure-remove-meal (:recipe-details/meal db)]}})))

(reg-event-fx
 :main/failure-remove-meal
 (fn [{:keys [db]} [_ failed-meal]]
   {:db
    (-> db
        (update :main/meal-plans conj failed-meal)
        (assoc :app/error "Fehler: Löschen fehlgeschlagen"))
    :app/timeout {:id :app/error-removal
                  :event [:app/remove-error]
                  :time 2000}}))

(reg-sub
 :main/recipes
 (fn [db _]
   (:main/recipes db)))

(defn sort-recipes [type-order recipes]
  (sort-by
   (fn [[recipe-type]] (->> type-order
                           (map-indexed #(vector %1 %2))
                           (some
                            (fn [[idx recipe-type-order]]
                              (when (= recipe-type-order recipe-type) idx)))))
   recipes))

(defn sorted-recipes [recipes]
  (->> recipes
       (group-by :type)
       (sort-recipes ["NORMAL" "NEW" "MISC" "FAST" "RARE"])))

(reg-sub
 :main/sorted-recipes
 :<- [:main/recipes]
 sorted-recipes)

(defn lunch-recipes [recipes]
  (->> recipes
       (group-by :type)
       (sort-recipes ["FAST" "NEW" "NORMAL" "MISC" "RARE"])))

(reg-sub
 :main/lunch-recipes
 :<- [:main/recipes]
 lunch-recipes)

(reg-sub
 :main/meal-plans
 (fn [db _]
   (:main/meal-plans db)))

(reg-sub
 :main/meals-without-shopping-list
 :<- [:main/weekly-meal-plans]
 (fn [meals-plans]
   (filter #(and (not (:shopping-list %))
                 (:recipe %)
                 (or (isAfter (:date %) (startOfDay (js/Date.)))
                     (= (:date %) (startOfDay (js/Date.)))))
           (flatten meals-plans))))

(reg-sub
 :main/start-of-week
 (fn [db _]
   (:main/start-of-week db)))

(defn group-meal-plans [meal-plans]
  (->> meal-plans
       (group-by :date)
       (map #(hash-map (first %) (->> (second %)
                                      (group-by :type)
                                      (map (fn [[type [recipe]]] (hash-map type recipe)))
                                      (apply merge))))
       (apply merge)))

(reg-sub
 :main/weekly-meal-plans
 :<- [:main/meal-plans]
 :<- [:main/start-of-week]
 (fn [[meal-plans start-date]]
   (map
    (fn [day]
      (let [date (startOfDay (addDays start-date day))]
        [(get-in (group-meal-plans meal-plans)
                 [date :meal-type/lunch]
                 {:date date
                  :type :meal-type/lunch})
         (get-in (group-meal-plans meal-plans)
                 [date :meal-type/dinner]
                 {:date date
                  :type :meal-type/dinner})]))
    (range 4))))

(reg-sub
 :main/bank-holidays
 (fn [db]
   (filter
    #(or (nil? (:states %)) ((:states %) :by))
    (:main/bank-holidays db))))

(reg-sub
 :main/bank-holiday
 :<- [:main/bank-holidays]
 (fn [bank-holidays [_ date]]
   (let [c-day (getDate date)
         c-month (getMonth date)]
     (some (fn [{:keys [month day name]}]
             (when
                 (and (= month (inc c-month))
                      (= day c-day))
               name))
           bank-holidays))))

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

(def routes
  [["/" {:name :route/main
         :view meal-plan
         :title "Essensplan"}]
   ["/meal-plan" {:name :route/meal-plan
                  :view meal-plan
                  :title "Essensplan"}]])
