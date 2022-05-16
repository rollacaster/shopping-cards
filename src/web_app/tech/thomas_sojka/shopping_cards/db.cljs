(ns tech.thomas-sojka.shopping-cards.db
  (:require [cljs.reader :refer [read-string]]
            [clojure.spec.alpha :as s]
            [re-frame.core :refer [dispatch]]
            [datascript.core :as d]))

(def conn (atom nil))

(def ws (new js/WebSocket (str "ws://" (.-host js/location) "/ws")))
(defmulti sync-handler (fn [message]
                         (first message)))
(defmethod sync-handler :db/schema [[_ schema]]
  (reset! conn @(d/create-conn schema)))
(defmethod sync-handler :db/bootstrap [[_ bootstrap-data]]
  (d/transact! conn bootstrap-data)
  (dispatch [:query
             {:q '[:find (pull ?r [[:recipe/id :as :id]
                                   [:recipe/name :as :name]
                                   [:recipe/image :as :image]
                                   [:recipe/link :as :link]
                                   {:recipe/type [[:db/ident]]}])
                   :where
                   [?r :recipe/id ]]
              :on-success [:main/success-recipes]
              :on-failure [:main/failure-recipes]}])
  (dispatch [:main/init-meal-plans (js/Date.)]))
(.addEventListener ws "message" (fn [event] (sync-handler (read-string (.-data event)))))
(.addEventListener ws "open"
                     (fn []
                       (.send ws [:db/schema])
                       (.send ws [:db/bootstrap])))

(s/def :app/route map?)
(s/def :app/loading boolean?)
(s/def :app/error (s/nilable string?))

(s/def :recipe/id string?)
(s/def :recipe/name string?)
(s/def :recipe/type #{:recipe-type/normal :recipe-type/new :recipe-type/misc :recipe-type/fast :recipe-type/rare})
(s/def :recipe/image string?)
(s/def :recipe/link (s/nilable string?))
(s/def :recipe/inactive boolean?)
(s/def :recipe/recipe (s/keys :req-un [:recipe/name :recipe/image]
                              :req [:recipe/type]
                              :opt-un [:recipe/link :recipe/inactive]))
(s/def :main/recipes (s/coll-of :recipe/recipe))

(s/def :meal-plan/date inst?)
(s/def :meal-plan/type #{:meal-type/dinner :meal-type/lunch})
(s/def :meal-plan/recipe :recipe/recipe)
(s/def :meal-plan/meal
  (s/keys :req-un [:meal-plan/date :meal-plan/type]
          :opt-un [:meal-plan/recipe]))
(s/def :main/meal-plans (s/coll-of :meal-plan/meal))

(s/def :main/start-of-week inst?)
(s/def :main/bank-holidays (s/coll-of map? :kind set?))

(s/def :ingredient/id string?)
(s/def :ingredient/category keyword?)
(s/def :ingredient/name string?)
(s/def :ingredient/ingredient (s/keys :req [:ingredient/id :ingredient/name]
                                      :opt [:ingredient/category]))
(s/def :shopping-card/read-ingredient (s/tuple :ingredient/id :ingredient/name))
(s/def :shopping-card/ingredients (s/coll-of :shopping-card/read-ingredient))
(s/def :shopping-card/selected-ingredient-ids (s/coll-of :ingredient/id :kind set?))

(s/def :extra-ingredients/filter string?)
(s/def :extra-ingredients/ingredients (s/coll-of :ingredient/ingredient))

(s/def :recipe-details/ingredients (s/coll-of :shopping-card/read-ingredient))
(s/def :recipe-details/meal (s/nilable :meal-plan/meal))

(s/def :app/db (s/keys :req [:app/error
                             :app/loading
                             :app/route
                             :shopping-card/selected-ingredient-ids
                             :shopping-card/ingredients
                             :extra-ingredients/filter
                             :recipe-details/ingredients
                             :recipe-details/meal
                             :main/recipes
                             :main/meal-plans
                             :main/start-of-week]))

(def default-db
  {:app/error nil
   :app/loading false
   :app/route {}
   :main/recipes []
   :main/ingredients []
   :main/meal-plans []
   :main/start-of-week (js/Date.)
   :main/bank-holidays #{}
   :shopping-card/selected-ingredient-ids #{}
   :shopping-card/ingredients []
   :extra-ingredients/filter ""
   :extra-ingredients/ingredients []
   :recipe-details/ingredients []
   :recipe-details/meal nil})
