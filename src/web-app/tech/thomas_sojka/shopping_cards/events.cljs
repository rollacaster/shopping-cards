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

(def check-spec-interceptor (after (partial check-and-throw :app/db)))
(reg-global-interceptor check-spec-interceptor)
(reg-fx :app/push-state
        (fn [route]
          (apply rfe/push-state route)))

(reg-event-db
 :app/navigate
 (fn [db [_ match]]
   (assoc db :app/route match)))

(reg-fx :app/scroll-to
        (fn [[x y]]
          (.scrollTo js/window x y)))

(reg-event-db
 :app/remove-error
 (fn [db] (assoc db :app/error nil)))

(reg-event-fx
 :app/initialise
 (fn [_ [_ year]]
   {:db default-db
    :http-xhrio {:method :get
                 :uri (str "https://raw.githubusercontent.com/lambdaschmiede/freitag/master/resources/com/lambdaschmiede/freitag/de/"
                           year
                           ".edn")
                 :response-format (ajax/text-response-format)
                 :on-success [:main/success-bank-holidays]
                 :on-failure [:main/failure-bank-holidays]}}))

(defonce timeouts (r/atom {}))

(reg-fx
 :app/timeout
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



(defn toggle-map [key map]
  ((if (map key) disj conj) map key))

(reg-event-db
 :shopping-card/toggle-selected-ingredients
 (fn [db [_ id]]
   (update
    db
    :shopping-card/selected-ingredient-ids
    (partial toggle-map id))))

;; TODO is the the :shopping-card entry?
(reg-event-fx
 :shopping-card/load-ingredients-for-meals
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   {:db (assoc db :app/loading true)
    :http-xhrio {:method :get
                 :uri (str "/ingredients?" (str/join "&" (map #(str "recipe-ids=" %) (map (comp :id :recipe) meals-without-shopping-list))))
                 :response-format (ajax/raw-response-format)
                 :on-success [:shopping-card/success-load-ingredients-for-selected-recipes]
                 :on-failure [:shopping-card/failure-load-ingredients-for-selected-recipes]}}))

(defn add-water [ingredients]
  (conj ingredients "6175d1a2-0af7-43fb-8a53-212af7b72c9c"))

(reg-event-fx
 :shopping-card/success-load-ingredients-for-selected-recipes
 (fn [{:keys [db]} [_ res]]
   {:app/push-state [:tech.thomas-sojka.shopping-cards.view/deselect-ingredients]
    :app/scroll-to [0 0]
    :db
    (let [data (read-string res)]
      (-> db
          (assoc :shopping-card/ingredients (vec data))
          (assoc :app/loading false)
          (assoc :shopping-card/selected-ingredient-ids (add-water (set (map first data))))))}))

(reg-event-db
 :shopping-card/failure-load-ingredients-for-selected-recipes
 (fn [db _]
   (assoc db :shopping-card/ingredients :ERROR)))

(defn shopping-card-ingredients [db]
  (let [{:keys [shopping-card/ingredients shopping-card/selected-ingredient-ids]}
        db]
    (->> ingredients
         (filter #(contains? selected-ingredient-ids (first %)))
         (map second))))

(reg-event-fx
 :shopping-card/create
 (fn [{:keys [db]} [_ meals-without-shopping-list]]
   {:db (assoc db :app/loading true)
    :http-xhrio {:method :post
                 :uri "/shopping-card"
                 :params (shopping-card-ingredients db)
                 :format (ajax/json-request-format)
                 :response-format (ajax/text-response-format)
                 :on-success [:shopping-card/success-shopping-card meals-without-shopping-list]
                 :on-failure [:shopping-card/failure-shopping-card]}}))

(reg-event-fx
 :shopping-card/failure-shopping-card
 (fn [{:keys [db]} _]
   {:app/push-state [:tech.thomas-sojka.shopping-cards.view/error]
    :db (assoc db :app/loading false)}))

(reg-event-fx
 :shopping-card/success-shopping-card
 (fn [_ [_ meals-without-shopping-list card-id]]
   ;; TODO Why is the client calling that?!
   {:http-xhrio {:method :post
                 :uri "/shopping-list"
                 :params (map (fn [{:keys [type date]}] [type date])
                              meals-without-shopping-list)
                 :format (ajax/json-request-format)
                 :response-format (ajax/text-response-format)
                 :on-success [:shopping-card/success-shopping-list meals-without-shopping-list card-id]
                 :on-failure [:shopping-card/failure-shopping-list]}}))

(reg-event-fx
 :shopping-card/success-shopping-list
 (fn [{:keys [db]} [_ meals-without-shopping-list card-id]]
   {:db (-> db
            (assoc :app/loading false)
            ;; TODO update to has shopping-list
            (update :main/meal-plans #(map (fn [meal-plan]
                                             (if
                                                 (some
                                                  (fn [meal-with-shopping-list]
                                                    (and
                                                     (= (:type meal-with-shopping-list) (:type meal-plan))
                                                     (= (:date meal-with-shopping-list) (:date meal-plan))))
                                                  meals-without-shopping-list)
                                               (assoc meal-plan :shopping-list true)
                                               meal-plan))
                                           (:main/meal-plans db)))
            (assoc :extra-ingredients {}))
    :app/push-state [:tech.thomas-sojka.shopping-cards.view/finish {:card-id card-id}]}))

(reg-event-db
 :extra-ingredients/filter-ingredients
 (fn [db [_ filter]]
   (assoc db :extra-ingredients/filter filter)))

(reg-event-fx
 :extra-ingredients/show
 (fn [_ _]
   {:app/push-state [:tech.thomas-sojka.shopping-cards.view/add-ingredients]
    :app/scroll-to [0 0]
    :http-xhrio {:method :get
                 :uri "/ingredients"
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:extra-ingredients/success-load-ingredients]
                 :on-failure [:extra-ingredients/success-failure-ingredients]}}))

(reg-event-db
 :extra-ingredients/success-load-ingredients
 (fn [db [_ ingredients]]
   (assoc db :extra-ingredients/ingredients ingredients)))

(reg-event-fx
 :extra-ingredients/add
 (fn [{:keys [db]} [_ id name]]
   {:db (-> db
            (update :shopping-card/ingredients conj [id name])
            (update :shopping-card/selected-ingredient-ids conj id)
            (assoc :extra-ingredients/filter ""))
    :app/push-state [:tech.thomas-sojka.shopping-cards.view/deselect-ingredients]}))

(reg-event-fx
 :recipe-details/show-meal-details
 (fn [{:keys [db]} [_ meal]]
   {:app/push-state [:tech.thomas-sojka.shopping-cards.view/meal-plan-details]
    :db (assoc db :recipe-details/meal meal)
    :http-xhrio {:method :get
                 :uri (str "/recipes/" (:id (:recipe meal)) "/ingredients")
                 :response-format (ajax/raw-response-format)
                 :on-success [:recipe-details/success-load-ingredients-for-recipe]
                 :on-failure [:recipe-details/failure-load-ingredients-for-recipe]}}))

(reg-event-db
 :recipe-details/success-load-ingredients-for-recipe
 (fn [db [_ data]]
   (assoc db :recipe-details/ingredients (read-string data))))

(reg-event-db
 :recipe-details/failure-load-ingredients-for-recipe
 (fn [db _]
   (assoc db :recipe-details/ingredients :ERROR)))


