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

(reg-event-db
 :toggle-selected-recipes
 (fn [db [_ id]]
   (update
    db
    :selected-recipes
    (partial toggle-map id))))

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
 :show-recipe
 (fn [_ [_ id]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/recipe {:recipe-id id}]}))

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

(comment
  @re-frame.db/app-db
  (dispatch [:initialise-db])
  (dispatch [:load-recipes])
  (:recipes @re-frame.db/app-db)
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

