(ns tech.thomas-sojka.shopping-cards.views.sort-ingredients
  (:require [re-frame.core :as rf]
            ["react-beautiful-dnd" :as react-beautiful-dnd]
            [reagent.core :as r]))


(defn- reorder [items start-index end-index]
  (if (< start-index end-index)
    (reduce
     into
     [(subvec items 0 start-index)
      (subvec items (inc start-index) (inc end-index))
      [(get items start-index)]
      (subvec items (inc end-index))])
    (reduce
     into
     [(subvec items 0 end-index)
      [(get items start-index)]
      (subvec items end-index start-index)
      (subvec items (inc start-index))])))

(defn- sort-ingredients [{:keys [ingredients]}]
  (let [ingredients (r/atom (vec ingredients))]
    (fn []
      [:> react-beautiful-dnd/DragDropContext {:onDragEnd (fn [result]
                                                            (when (.-destination result)
                                                              (swap! ingredients
                                                                     #(reorder %
                                                                               ^js (.-source.index result)
                                                                               ^js (.-destination.index result)))))}
       [:> react-beautiful-dnd/Droppable {:droppableId "droppable"}
        (fn [provided snapshot]
          (let [{:keys [droppableProps innerRef]} (js->clj provided :keywordize-keys true)]
            (r/as-element
             [:div (merge droppableProps {:ref innerRef})
              [:ul.list.ph5
               (->> @ingredients
                    (map-indexed
                     (fn [index {:keys [:ingredient/name]}]
                       [:> react-beautiful-dnd/Draggable {:key name :draggableId name :index index}
                        (fn [provided snapshot]
                          (let [{:keys [draggableProps dragHandleProps innerRef]} (js->clj provided :keywordize-keys true)]
                            (r/as-element
                             [:li.ba.pa2.bg-orange-400.white.b.f3
                              (assoc-in
                               (merge {:ref innerRef} draggableProps dragHandleProps)
                               [:style :height] 50)
                              name])))])))]
              (.-placeholder provided)])))]])))

(defn main []
  (let [ingredients @(rf/subscribe [:ingredients])]
    (when (seq ingredients)
      [sort-ingredients {:ingredients ingredients}])))
