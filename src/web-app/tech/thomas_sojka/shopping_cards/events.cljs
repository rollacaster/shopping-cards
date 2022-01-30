(ns tech.thomas-sojka.shopping-cards.events
  (:require
   [ajax.core :as ajax]
   [cljs.reader :refer [read-string]]
   [cljs.spec.alpha :as s]
   [clojure.string :as str]
   [re-frame.core
    :refer [after
            dispatch
            reg-event-db
            reg-event-fx
            reg-fx
            reg-global-interceptor]]
   [reagent.core :as r]
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

(reg-event-fx
 :initialise
 (fn [_ [_ year]]
   {:db default-db
    :http-xhrio {:method :get
                 :uri (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                           year
                           ".edn")
                 :response-format (ajax/text-response-format)
                 :on-success [:success-bank-holidays]
                 :on-failure [:failure-bank-holidays]}}))

(defonce timeouts (r/atom {}))

(reg-fx
  :timeout
  (fn [{:keys [id event time]}]
    (when-some [existing (get @timeouts id)]
      (js/clearTimeout existing)
      (swap! timeouts dissoc id))
    (when (some? event)
      (swap! timeouts assoc id
        (js/setTimeout
          (fn []
            (dispatch event))
          time)))))

(reg-event-db
  :success-bank-holidays
  (fn [db [_ data]]
    (assoc db :bank-holidays (read-string data))))

(reg-event-db
  :filter-ingredients
  (fn [db [_ filter]]
    (assoc db :ingredient-filter filter)))

(reg-event-db
  :failure-bank-holidays
  (fn [db _]
    ;; TODO Handle failed bank holiday loading
    db))

(defn toggle-map [key map]
  ((if (map key) disj conj) map key))

(reg-event-db
 :toggle-selected-ingredients
 (fn [db [_ id]]
   (update
    db
    :selected-ingredients
    (partial toggle-map id))))

(reg-event-fx
 :load-recipes
 (fn [{:keys [db]}]
   (if (empty? (:recipes db))
     {:db (assoc db :loading true)
      :http-xhrio {:method :get
                   :uri "recipes"
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:success-recipes]
                   :on-failure [:failure-recipes]}}
     db)))

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
                 :on-failure [:failure-add-meal (:selected-meal db)]}}))

(defn remove-meal [meal-plans {:keys [date type]}]
  (remove (fn [m] (and (= date (:date m))
                      (= type (:type m))))
          meal-plans))

(reg-event-fx
 :failure-add-meal
 (fn [{:keys [db]} [_ failed-meal]]
   {:db (-> db
            (update :meal-plans remove-meal failed-meal)
            (assoc :error "Fehler: Speichern fehlgeschlagen"))
    :timeout {:id :error-removal
              :event [:remove-error]
              :time 2000}}))

(reg-event-fx
 :load-ingredients-for-meals
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   {:db (assoc db :loading true)
    :http-xhrio {:method :get
                 :uri (str "/ingredients?" (str/join "&" (map #(str "recipe-ids=" %) (map (comp :id :recipe)meals-without-shopping-list))))
                 :response-format (ajax/raw-response-format)
                 :on-success [:success-load-ingredients-for-selected-recipes]
                 :on-failure [:failure-load-ingredients-for-selected-recipes]}}))

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
          (assoc :recipe-ingredients (vec data))
          (assoc :loading false)
          (assoc :selected-ingredients (add-water (set (map first data))))))}))

(reg-event-db
 :failure-load-ingredients-for-selected-recipes
 (fn [db _]
   (assoc db :recipe-ingredients :ERROR)))

(reg-event-db
 :success-load-ingredients-for-recipe
 (fn [db [_ data]]
   (assoc db :recipe-details (read-string data))))

(reg-event-db
 :failure-load-ingredients-for-recipe
 (fn [db _]
   (assoc db :recipe-details :ERROR)))

(reg-event-fx
 :show-add-ingredients
 (fn [_ _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/add-ingredients]
    :scroll-to [0 0]
    :http-xhrio {:method :get
                 :uri "/ingredients"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:success-load-ingredients]
                 :on-failure [:success-failure-ingredients]}}))
(reg-event-db
  :success-load-ingredients
  (fn [db [_ ingredients]]
    (assoc db :ingredients ingredients)))

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
 :show-meal-details
 (fn [{:keys [db]} [_ meal]]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan-details]
    :db (assoc db :selected-meal meal)
    :http-xhrio {:method :get
                 :uri (str "/recipes/" (:id (:recipe meal)) "/ingredients")
                 :response-format (ajax/raw-response-format)
                 :on-success [:success-load-ingredients-for-recipe]
                 :on-failure [:failure-load-ingredients-for-recipe]}}))

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
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   (let [{:keys [recipe-ingredients
                 selected-ingredients
                 extra-ingredients]}
         db]
     {:db (assoc db :loading true)
      :http-xhrio {:method :post
                   :uri "/shopping-card"
                   :params (->> recipe-ingredients
                                (filter #(contains? selected-ingredients (first %)))
                                (map second)
                                (concat (vals extra-ingredients)))
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-success [:success-shopping-card meals-without-shopping-list]
                   :on-failure [:failure-shopping-card]}})))

(reg-event-fx
  :failure-shopping-card
 (fn [{:keys [db]} _]
   {:push-state [:tech.thomas-sojka.shopping-cards.view/error]
    :db (assoc db :loading false)}))

(reg-event-fx
 :success-shopping-card
 (fn [{:keys [db]} [_ meals-without-shopping-list card-id]]
   (if meals-without-shopping-list
     {:http-xhrio {:method :post
                   :uri "/shopping-list"
                   :params (map (fn [{:keys [type date]}] [type date])
                                meals-without-shopping-list)
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-success [:success-shopping-list meals-without-shopping-list]
                   :on-failure [:failure-shopping-list]}}
     {:push-state [:tech.thomas-sojka.shopping-cards.view/finish {:card-id card-id}]
      :db (assoc db :loading false)})))

(reg-event-fx
 :success-shopping-list
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   {:db (-> db
            (assoc :loading false)
            ;; TODO update to has shopping-list
            (update :meal-plans #(map (fn [meal-plan]
                                        (if
                                            (some
                                             (fn [meal-with-shopping-list]
                                               (and
                                                (= (:type meal-with-shopping-list) (:type meal-plan))
                                                (= (:date meal-with-shopping-list) (:date meal-plan))))
                                             meals-without-shopping-list)
                                          (assoc meal-plan :shopping-list true)
                                          meal-plan))
                                      (:meal-plans db)))
            (assoc :extra-ingredients {}))
    :push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan]}))

(reg-event-fx
 :remove-meal
 (fn [{:keys [db]}]
   (let [{:keys [date type]} (:selected-meal db)]
     {:db
      (update db :meal-plans remove-meal (:selected-meal db))
      :push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan]
      :http-xhrio {:method :delete
                   :uri "/meal-plans"
                   :url-params {:date (.toISOString date) :type type}
                   :format (ajax/json-request-format)
                   :response-format (ajax/text-response-format)
                   :on-failure [:failure-remove-meal (:selected-meal db)]}})))

(reg-event-fx
 :failure-remove-meal
 (fn [{:keys [db]} [_ failed-meal]]
   {:db
    (-> db
        (update :meal-plans conj failed-meal)
        (assoc :error "Fehler: Löschen fehlgeschlagen"))
    :timeout {:id :error-removal
              :event [:remove-error]
              :time 2000}}))

(reg-event-db
 :remove-error
 (fn [db] (assoc db :error nil)))

(reg-event-fx
 :add-extra-ingredient
 (fn [{:keys [db]} [_ id name]]
   {:db (-> db
            (update :recipe-ingredients conj [id name])
            (update :selected-ingredients conj id)
            (assoc :ingredient-filter ""))
    :push-state [:tech.thomas-sojka.shopping-cards.view/deselect-ingredients]}))


