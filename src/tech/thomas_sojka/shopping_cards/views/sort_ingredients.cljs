(ns tech.thomas-sojka.shopping-cards.views.sort-ingredients
  (:require [re-frame.core :as rf]
            ["react-beautiful-dnd" :as react-beautiful-dnd]
            [reagent.core :as r]))

(defn ingredient []
  [:ul
   (let [ingredients @(rf/subscribe [:ingredients])]
     (->> ingredients
          (map-indexed
           (fn [index {:keys [:ingredient/name]}]
             [:> react-beautiful-dnd/Draggable {:key name :draggableId name
                                                :index index}
              (fn [provided snapshot]
                (let [{:keys [draggableProps dragHandleProps innerRef]} (js->clj provided :keywordize-keys true)]
                  (r/as-element
                   [:li.ba
                    (assoc-in
                     (merge
                      {:ref innerRef}
                      draggableProps
                      dragHandleProps)
                     [:style :height] 50)
                    name])))]))))])

(defn main []
  [:div
   [:h1 "Sort Ingredients"]
   [:> react-beautiful-dnd/DragDropContext {:onDragEnd (fn [e] (prn e))}
    [:> react-beautiful-dnd/Droppable {:droppableId "droppable"}
     (fn [provided snapshot]
       (let [{:keys [droppableProps innerRef]} (js->clj provided :keywordize-keys true)]
         (r/as-element
          [:div
           (merge droppableProps
                  {:ref innerRef})
           [ingredient]
           (.-placeholder provided)])))]]])
