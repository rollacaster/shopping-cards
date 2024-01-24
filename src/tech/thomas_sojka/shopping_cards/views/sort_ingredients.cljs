(ns tech.thomas-sojka.shopping-cards.views.sort-ingredients
  (:require [re-frame.core :as rf]
            ["react-beautiful-dnd" :as react-beautiful-dnd]
            [reagent.core :as r]
            ["react-tooltip" :as react-tooltip]
            [tech.thomas-sojka.shopping-cards.shopping-items :as shopping-items]))
(def tooltip react-tooltip/Tooltip)

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

(defn find-recipes-by-ingredient-id [recipes ingredient-id]
  (->> recipes
       (filter (fn [recipe]
                 ((set
                   (map :ingredient/id
                        (:recipe/cooked-with recipe)))
                  ingredient-id)))
       (mapv :recipe/name)))
(defn- sort-ingredients [{:keys [ingredients]}]
  (let [recipes @(rf/subscribe [:all-recipes])
        ingredients
        (r/atom (vec (->> ingredients
                          (map (fn [ingredient]
                                 (assoc ingredient :ingredient/recipes
                                        (find-recipes-by-ingredient-id
                                         recipes
                                         (:ingredient/id ingredient)))))
                          (sort-by (fn [{:ingredient/keys [recipes]}] (count recipes)))
                          (sort-by (fn [{:keys [ingredient/category]}] category)
                                   (fn [category1 category2]
                                     (< (.indexOf shopping-items/penny-order category1)
                                        (.indexOf shopping-items/penny-order category2)))))))]
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
              [:button
               {:on-click #(prn (mapv :ingredient/id @ingredients))}
               "Copy Sort Order"]
              [:ul.list.ph5
               (->> @ingredients

                    (map-indexed
                     (fn [index {:ingredient/keys [id name category recipes]}]
                       ^{:key id}
                       [:> react-beautiful-dnd/Draggable {:key name :draggableId name :index index}
                        (fn [provided snapshot]
                          (let [{:keys [draggableProps dragHandleProps innerRef]} (js->clj provided :keywordize-keys true)]
                            (r/as-element
                             [:li.ba.pa2.bg-orange-400.white.flex.justify-between.items-center.w-100
                              (assoc-in
                               (merge {:ref innerRef} draggableProps dragHandleProps)
                               [:style :height] 50)
                              [:button.fw6.f3.bn.bg-transparent
                               {:on-click #(prn id)}
                               name]
                              [:div
                               [:span.fw3.mr3
                                {:class (str "tooltip" index)}
                                (str "Rezepte (" (count recipes) ")")
                                [:> tooltip {:anchor-select (str ".tooltip" index) :place "top"}
                                 [:div.pa2
                                  (map (fn [recipe]
                                         ^{:key recipe}
                                         [:div
                                          [:span.fw3 recipe]])
                                       recipes)]]]
                               [:span.fw3 category]]])))])))]
              (.-placeholder provided)])))]])))
(defn main []
  (let [ingredients @(rf/subscribe [:ingredients])]
    (when (seq ingredients)
      [sort-ingredients {:ingredients ingredients}])))
