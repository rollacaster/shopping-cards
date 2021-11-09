(ns tech.thomas-sojka.shopping-cards.events
  (:require [ajax.core :as ajax]
            [cljs.reader :refer [read-string]]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-fx after reg-global-interceptor]]
            [reitit.frontend.easy :as rfe]
            [tech.thomas-sojka.shopping-cards.db :refer [default-db]]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (prn (s/explain-str a-spec db))
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :tech.thomas-sojka.shopping-cards.db/db)))
(reg-global-interceptor check-spec-interceptor)
(reg-fx :push-state
  (fn [route]
    (apply rfe/push-state route)))

(reg-fx :scroll-to
  (fn [[x y]]
    (.scrollTo js/window x y)))

(reg-event-db
 :initialise-db
 (fn [] default-db))

(defn toggle-map [key map]
  ((if (map key) disj conj) map key))

(reg-event-db
 :toggle-selected-ingredients
 (fn [db [_ id]]
   (update
    db
    :selected-ingredients
    (partial toggle-map id))))

(reg-event-db
 :add-recipes
 (fn [db [_ recipes]] (-> db
                         (assoc :recipe recipes)
                         (assoc :loading false))))

(reg-event-db
 :add-ingredients
 (fn [db [_ ingredients]] (-> db
                             (assoc :ingredient ingredients)
                             (assoc :loading false))))

(reg-event-fx
 :load-recipes
 (fn [{:keys [db]}]
   {:db (assoc db :loading true)
    :http-xhrio {:method :get
                 :uri "recipes"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:success-recipes]
                 :on-failure [:failure-recipes]}}))

(reg-event-db
 :success-recipes
 (fn [db [_ data]]
   (-> db
       (assoc :loading false)
       (assoc :recipes data))))

(reg-event-db
 :failure-recipes
 (fn [db _]
   (-> db
       (assoc :loading false)
       (assoc :recipes :ERROR))))

(reg-event-fx
 :init-meal-plans
 (fn [{:keys [db]} [_ start-of-week]]
   (if (nil? (->> (:meal-plans db)
                  (map #(.getMonth (:date %)))
                  (some #(= (.getMonth start-of-week) %))))
     {:db (-> db
              (assoc :loading true)
              (assoc :start-of-week start-of-week))
      :http-xhrio {:method :get
                   :uri (str "/meal-plans/" (inc (.getMonth start-of-week)))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:success-meal-plans]
                   :on-failure [:failure-meal-plans]}}
     {:db (assoc db :start-of-week start-of-week)})))

(reg-event-db
 :success-meal-plans
 (fn [db [_ data]]
   (-> db
       (assoc :loading false)
       (update :meal-plans
               concat
               (map (fn [meal-plan]
                      (-> meal-plan
                          (update :type keyword)
                          (update :date #(js/Date. %))))
                    data)))))

(reg-event-db
 :failure-meal-plans
 (fn [db _]
   (-> db
       (assoc :loading false)
       (assoc :meal-plans :ERROR))))

(reg-event-db
 :toggle-selected-recipes
 (fn [db [_ id]]
   (update
    db
    :selected-recipes
    (partial toggle-map id))))

(reg-event-fx
 :add-meal
 (fn [{:keys [db]} [_ recipe]]
   {:db (-> db
            (update :meal-plans conj (assoc (:selected-meal db) :recipe recipe))
            (assoc :selected-meal nil))
    :push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan]
    :http-xhrio {:method :post
                 :uri "/meal-plans"
                 :params (assoc (:selected-meal db) :recipe recipe)
                 :format (ajax/json-request-format)
                 :response-format (ajax/text-response-format)
                 :on-failure [:failure-add-meal]}}))

(reg-event-db
 :failure-add-meal
 (fn []
   (prn "Fail")))

(reg-event-fx
 :load-ingredients-for-selected-recipes
 (fn [{:keys [db]} _]
   (let [{:keys [selected-recipes]} db]
     {:db (assoc db :loading true)
      :http-xhrio {:method :get
                   :uri (str "/ingredients?" (str/join "&" (map #(str "recipe-ids=" %) selected-recipes)))
                   :response-format (ajax/raw-response-format)
                   :on-success [:success-load-ingredients-for-selected-recipes]
                   :on-failure [:failure-load-ingredients-for-selected-recipes]}})))

(defn add-water [ingredients]
  (conj ingredients "6175d1a2-0af7-43fb-8a53-212af7b72c9c"))

(reg-event-fx
 :success-load-ingredients-for-selected-recipes
 (fn [{:keys [db]} [_ res]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/deselect-ingredients]
    :scroll-to [0 0]
    :db
    (let [data (read-string res)]
      (-> db
          (assoc :ingredients data)
          (assoc :loading false)
          (assoc :selected-ingredients (add-water (set (map first data))))))}))

(reg-event-db
 :failure-load-ingredients-for-selected-recipes
 (fn [db _]
   (assoc db :ingredients :ERROR)))

(reg-event-fx
 :load-ingredients-for-recipe
 (fn [{:keys [db]} [_ recipe-id]]
   {:db (assoc db :loading true)
    :http-xhrio {:method :get
                 :uri (str "/recipes/" recipe-id "/ingredients")
                 :response-format (ajax/raw-response-format)
                 :on-success [:success-load-ingredients-for-recipe]
                 :on-failure [:failure-load-ingredients-for-recipe]}}))

(reg-event-db
 :success-load-ingredients-for-recipe
 (fn [db [_ data]]
   (assoc db :recipe-details (read-string data))))

(reg-event-db
 :failure-load-ingredients-for-recipe
 (fn [db _]
   (assoc db :recipe-details :ERROR)))

(reg-event-fx
 :show-recipes
 (fn [_ _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/recipes]}))

(reg-event-fx
 :show-meal-plan
 (fn [_ _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan]}))

(reg-event-fx
 :show-main
 (fn [_ _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/main]}))

(reg-event-fx
 :select-meal
 (fn [{:keys [db]} [_ meal]]
   {:push-state
    (if (= (:type meal) :meal-type/lunch)
      [:tech.thomas-sojka.shopping-cards.view/select-lunch]
      [:tech.thomas-sojka.shopping-cards.view/select-dinner])
    :db (assoc db :selected-meal meal)}))

(reg-event-fx
 :select-dinner
 (fn [{:keys [db]} [_ meal]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/select-dinner]
    :db (assoc db :selected-meal meal)}))

(reg-event-fx
 :show-recipe
 (fn [_ [_ id]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/recipe {:recipe-id id}]}))

(reg-event-fx
 :show-meal-details
 (fn [{:keys [db]} [_ meal]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/recipe-details {:recipe-id (:id (:recipe meal))}]
    :db (assoc db :selected-meal meal)}))

(reg-event-fx
 :restart
 (fn [{:keys [db]} _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/main]
    :db (-> db
            (assoc :selected-ingredients #{})
            (assoc :selected-recipes #{}))}))

(reg-event-db
 :navigate
 (fn [db [_ match]]
   (assoc db :route match)))

(reg-event-fx
 :create-shopping-card
 (fn [{:keys [db]} _]
   (let [{:keys [ingredients selected-ingredients]} db]
     {:db (assoc db :loading true)
      :http-xhrio {:method :post
                   :uri "/shopping-card"
                   :params (->> ingredients
                                (filter #(contains? selected-ingredients (first %)))
                                (map second))
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-success [:success-shopping-card]
                   :on-failure [:failure-shopping-card]}})))

(reg-event-fx
 :success-shopping-card
 (fn [{:keys [db]} [_ card-id]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/finish {:card-id card-id}]
    :db (assoc db :loading false)}))

(reg-event-fx
 :failure-shopping-card
 (fn [{:keys [db]} _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/error]
    :db (assoc db :loading false)}))

(reg-event-fx
 :remove-meal
 (fn [{:keys [db]}]
   (let [{:keys [date type]} (:selected-meal db)]
     {:db
      (update db :meal-plans #(remove (fn [m] (and (= date (:date m))
                                                  (= type (:type m))))
                                      %))
      :push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan]
      :http-xhrio {:method :delete
                   :uri "/meal-plans"
                   :url-params {:date (.toISOString date) :type type}
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-failure [:failure-remove-meal]}})))

(reg-event-db
 :failure-remove-meal
 (fn []
   (prn "Fail")))

(comment
  (dispatch [:remove-meal
             {:date #inst "2021-10-28T06:11:18.005-00:00"
              :type :LUNCH}])
  (:selected-meal @re-frame.db/app-db)
  (dispatch [:initialise-db])
  (dispatch [:load-recipes])
  (:meal-plans @re-frame.db/app-db)
  (dispatch [:toggle-selected-recipes "d47bc268-5e9d-45da-af96-143b12d334c5"])
  (:selected-recipes @re-frame.db/app-db)
  (dispatch [:load-ingredients-for-selected-recipes])
  (:selected-ingredients @re-frame.db/app-db)
  (dispatch [:toggle-selected-ingredients "7cc3f4e2-fc7a-41d5-a2c8-65e53d9ad641"])
  (:selected-ingredients @re-frame.db/app-db)
  (dispatch [:load-ingredients-for-recipe "d47bc268-5e9d-45da-af96-143b12d334c5"])
  (:recipe-details @re-frame.db/app-db)
  (dispatch [:create-shopping-card])
  (dispatch [:show-recipes])
  (dispatch [:show-recipe "d0bb942b-0165-417d-9153-6c770c036fe8"])
  (dispatch [:show-main])
  (dispatch [:restart])
  )


