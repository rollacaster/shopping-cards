(ns tech.thomas-sojka.shopping-cards.edit-recipes.views
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [tech.thomas-sojka.shopping-cards.components
    :refer
    [recipe recipe-details]]))

(defn recipes-editing []
  (let [recipes @(subscribe [:main/recipes])]
    [:<>
     (->> recipes
          (sort-by :name)
          (map
           (fn [{:keys [id name image]}]
             ^{:key id}
             [recipe {:name name :image image :on-click #(dispatch [:edit-recipe/show-recipe id])}])))]))

(defn recipe-editing [match]
  (let [{:keys [path]} (:parameters match)
        {:keys [recipe-id]} path
        {:keys [name link image]} @(subscribe [:edit-recipe/recipe-details recipe-id])
        ingredients @(subscribe [:edit-recipe/ingredients recipe-id])]
    [recipe-details
     {:name name
      :link link
      :image image
      :ingredients ingredients
      :on-remove #(prn "deleted")}]))
