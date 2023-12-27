(ns clean-amount-desc
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [medley.core :as medley]
            [mentat.clerk-utils.show :refer [show-sci]]
            [nextjournal.clerk :as clerk]))

(def ingredients (json/parse-string (slurp "resources/public/ingredients.json") true))
(show-sci
 (defn index-by [f coll]
   (persistent! (reduce #(assoc! %1 (f %2) %2) (transient {}) coll)))
 (def id->ingredient (index-by :ingredient/id ingredients)))

(clerk/table ingredients)
(def parsed-recipes (json/parse-string (slurp "resources/public/recipes.json") true))

^{::clerk/sync true}
(defonce !recipes
  (atom parsed-recipes))
(comment
  (reset! !recipes
          (json/parse-string (slurp "resources/public/recipes.json") true))
  )
^{::clerk/sync true}
(defonce !recipe-updates
  (atom []))
(comment
  (reset! !recipe-updates []))
(defonce __init
  (add-watch !recipe-updates :store
             (fn [_ _ _ new-value]
               (spit "recipe-updates.edn" (prn-str new-value)))))

^{::clerk/sync true}
(defonce !ingredients (atom ingredients))

(clerk/table @!recipes)

#_(clerk/table(mapcat (fn [recipe]
                      (map
                       (fn [c] (-> c
                                  (dissoc :cooked-with/ingredient)
                                  (assoc :ingredient/name (:ingredient/name (id->ingredient (:cooked-with/ingredient c))))))
                       (:recipe/cooked-with recipe)))
                    @!recipes))

(def amount-desc-replacements
   [["kl. Dose/n" "Dose"]
    ["kl. Dose" "Dose"]
    ["eine halbe Dose" "Dose"]
    ["m.-großer" ""]
    ["m.-große" ""]
    ["großes" ""]
    ["große" ""]
    ["groß" ""]
    ["rote Zwiebel" ""]
    ["Zehe/n" "Zehen"]
    ["Knoblauchzehe" "Zehen"]
    ["Vollkorn-Tortillafladen" ""]
    ["½" ""]
    ["Tasse(n) vorgegarter Weizen" "Tasse"]
    ["mittelalter" ""]
    ["Paprikaschote" ""]
    ["kleine" ""]
    ["Karotte" ""]
    ["reife Avocado" ""]
    ["Stück, klein" ""]
    ["Stück, mittel" ""]
    ["Stück, mittelgroß" ""]
    ["Stück, rot" ""]
    ["Stück, " ""]])

(show-sci
 (def ingredient-name->ingredient (medley/index-by :ingredient/name @!ingredients)))

(show-sci
 (defn is-ingredient? [c ingredient-name]
   (= (:cooked-with/ingredient c)
      (:ingredient/id (ingredient-name->ingredient ingredient-name)))))

(show-sci
 (defn replace-amount-desc [{:keys [cooked-with/amount-desc] :as c}]
   (if-let [[included  replacement]  (some
                                      (fn [[included  replacement]]
                                        (when (and amount-desc (str/includes? amount-desc included))
                                          [included  replacement]))
                                      amount-desc-replacements)]
     (update c :cooked-with/amount-desc
             (fn [s]
               (-> s
                   (str/replace included replacement)
                   str/trim)))
     c))
 (defn separate-amount-desc [{:keys [cooked-with/amount-desc cooked-with/amount cooked-with/unit] :as c}]
   (cond-> c
     (and amount amount-desc unit (str/includes? amount-desc (str (int amount) unit)))
     (assoc :cooked-with/amount-desc (str (int amount) " " unit))))
 (defn- fix-amount-and-unit-but-desc [{:keys [cooked-with/amount-desc] :as c}]
   (-> c
       (assoc :cooked-with/amount (js/parseFloat (re-find #"\d+" amount-desc)))
       (assoc :cooked-with/unit (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc))))
 (defn extract-unit [{:keys [cooked-with/amount-desc cooked-with/amount cooked-with/unit] :as c}]
   (cond-> c
     (and amount-desc amount (not unit) (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc))
     (assoc :cooked-with/unit (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc)))))
(show-sci
 (defn fix-original [{:keys [cooked-with/amount cooked-with/amount-desc cooked-with/unit] :as c}]
   (cond-> c
     ;; no unit in amount-desc
     (and unit amount (not (str/includes? amount-desc unit)))
     (assoc :cooked-with/amount-desc (str (int amount) " " unit))

     ;; amount-desc is "etwas"
     (= amount-desc "etwas")
     (dissoc :cooked-with/amount-desc)

     (= amount-desc "n. B.")
     (dissoc :cooked-with/amount-desc)

     :true
     replace-amount-desc

     ;; Unit but no amount
     (and unit (not amount) (re-find #"\d+" amount-desc))
     (assoc :cooked-with/amount (js/parseFloat (re-find #"\d+" amount-desc)))

     (= amount-desc "0.25 TL")
     (dissoc :cooked-with/amount-desc :cooked-with/unit :cooked-with/amount)

     ;; amount-desc with no unit but it exists
     (and amount unit (= amount-desc (str (int amount))))
     (assoc :cooked-with/amount-desc (str (int amount) " " unit))

     ;; Fix amount-desc with zehe
     (and amount-desc (str/includes? amount-desc "zehe"))
     (assoc :cooked-with/unit "zehe")

     ;; No amount and unit but amount-desc
     (and amount-desc (not amount) (not unit) (re-find #"\d+" amount-desc) (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc))
     fix-amount-and-unit-but-desc

     :always
     extract-unit

     ;; unit and amount sticked together
     :true
     separate-amount-desc)))
(show-sci
 (defn fix-issue [{:keys [cooked-with/amount cooked-with/amount-desc cooked-with/unit] :as c}]
   (cond-> c
     ;; amount-desc with no unit but it exists
     (and amount unit (= amount-desc (str (int amount))))
     (assoc :cooked-with/amount-desc (str (int amount) " " unit))

     ;; No amount and unit but amount-desc
     (and amount-desc (not amount) (not unit) (re-find #"\d+" amount-desc) (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc))
     fix-amount-and-unit-but-desc

     ;; No amount and unit but amount-desc
     (and amount-desc (not amount) (not unit) (re-find #"\d+" amount-desc) (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc))
     fix-amount-and-unit-but-desc

     ;; Unit but no amount
     (and amount-desc (not amount) (not unit) (re-find #"\d+" amount-desc))
     (assoc :cooked-with/amount (js/parseFloat (re-find #"\d+" amount-desc)))

     ;; ingredient name in amount-desc
     (and amount-desc (str/includes? amount-desc (:ingredient/name c)))
     (assoc :cooked-with/amount-desc (str/trim (str/replace amount-desc (:ingredient/name c) ""))))))

(show-sci
 (defn update-cooked-with [recipes recipe-id ingredient-id field amount]
   (mapv
    (fn [recipe]
      (if (= (:recipe/id recipe) recipe-id)
        (assoc recipe
               :recipe/cooked-with
               (mapv
                (fn [c]
                  (if (= (:cooked-with/ingredient c) ingredient-id)
                    (assoc c field amount)
                    c))
                (:recipe/cooked-with recipe)))
        recipe))
    recipes))

 (def remove-unit-and-amount #{"Kräuter" "Zitrone" "Salat" "Curry" "Salz" "Pfeffer"
                               "Kümmel" "Petersilie" "Currypaste" "Paprikapulver" "Garam Masala"})
 (defn change-unit [c]
   (cond
     (and (is-ingredient? c "Tomatenmark")
          (= (:cooked-with/unit c) "g"))
     (-> c
         (assoc :cooked-with/unit "EL")
         (update :cooked-with/amount (fn [amount] (max 1 (int (/ amount 25))))))

     (and (is-ingredient? c "Mais")
          (or (= (:cooked-with/unit c) "g")
              (nil? (:cooked-with/unit c)))
          (:cooked-with/amount c))
     (-> c
         (assoc :cooked-with/unit "Dose")
         (assoc :cooked-with/amount 1))

     (and (is-ingredient? c "Frühlingszwiebel")
          (= (:cooked-with/unit c) "g"))
     (-> c
         (update :cooked-with/amount #(/ % 50))
         (dissoc :cooked-with/unit ))

     (is-ingredient? c "Frühlingszwiebel")
     (dissoc c :cooked-with/unit )

     (and (is-ingredient? c "Schmand")
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 15)))

     (and (is-ingredient? c "Parmesan")
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 5)))

     (and (is-ingredient? c "Linsen")
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 15)))

     (and (is-ingredient? c "Parmesan")
          (= (:cooked-with/unit c) "Handvoll"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c "Schmand")
          (= (:cooked-with/unit c) "Becher"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 200)))

     (and (is-ingredient? c "Karotte")
          (= (:cooked-with/unit c) "g"))
     (-> c
         (dissoc :cooked-with/unit)
         (update :cooked-with/amount #(js/Math.ceil (/ % 200))))

     (and (is-ingredient? c "Reis")
          (= (:cooked-with/unit c) "Tasse"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 150)))

     (and (is-ingredient? c "Butter")
          (= (:cooked-with/unit c) "EL"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c "Semmel")
          (= (:cooked-with/unit c) "g"))
     (-> c
         (dissoc :cooked-with/unit)
         (update :cooked-with/amount (fn [amount] (max 1 (int (/ amount 60))))))

     (and (is-ingredient? c "Wasser")
          (= (:cooked-with/unit c) "g"))
     (assoc c :cooked-with/unit "ml")

     (and (is-ingredient? c "Geriebener Käse")
          (= (:cooked-with/unit c) "Packung"))
     (-> c
         (assoc :cooked-with/unit "g")
         (assoc :cooked-with/amount 250))

     (and (is-ingredient? c "Spinat")
          (nil? (:cooked-with/unit c))
          (:cooked-with/amount c))
     (-> c
         (assoc :cooked-with/unit "g"))

     (and (is-ingredient? c "Wasser")
          (= (:cooked-with/unit c) "Tasse"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (assoc :cooked-with/amount 250))

     (and (is-ingredient? c "Kokosmilch")
          (= (:cooked-with/unit c) "Dose"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (update :cooked-with/amount (fn [amount] (int (* amount 400)))))

     (and (is-ingredient? c "Zucker")
          (= (:cooked-with/unit c) "Prise"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c "Hefe")
          (= (:cooked-with/unit c) "g"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c "Griechischer Joghurt")
          (nil? (:cooked-with/unit c)))
     (assoc c :cooked-with/unit "g" :cooked-with/amount "200")

     (is-ingredient? c "Nudeln")
     (-> c
         (assoc :cooked-with/unit "g"))

     (is-ingredient? c "Mozzarella")
     (-> c
         (assoc :cooked-with/unit "Packung")
         (assoc :cooked-with/amount 1))

     (is-ingredient? c "Kräuter-Frischkäse")
     (-> c
         (assoc :cooked-with/unit "Packung")
         (assoc :cooked-with/amount 1))

     (and (is-ingredient? c "Paprika")
          (= (:cooked-with/unit c) "Stück"))
     (dissoc c :cooked-with/unit)

     (and (is-ingredient? c "Toast")
          (= (:cooked-with/unit c) "Stk"))
     (dissoc c :cooked-with/unit)

     (and (is-ingredient? c "Zwiebel")
          (= (:cooked-with/unit c) "Stück"))
     (dissoc c :cooked-with/unit)

     (is-ingredient? c "Eier")
     (-> (dissoc c :cooked-with/unit)
         (update :cooked-with/amount js/Math.ceil))



     (some #(is-ingredient? c %)
           remove-unit-and-amount)
     (-> c
         (dissoc :cooked-with/unit :cooked-with/amount))

     :else c))

 (defn- cooked-with-component [c recipe class]
   [:tr
    {:class class}
    [:input.border.px-2.w-16
     {:value (:cooked-with/amount c)
      :on-change
      (fn [e]
        (swap! !recipes update-cooked-with
               (:recipe/id recipe)
               (:cooked-with/ingredient c)
               :cooked-with/amount
               (-> e .-target .-value int)))}]
    [:td
     [:input.border.px-2.w-16
      {:value (:cooked-with/unit c)
       :on-change
       (fn [e]
         (swap! !recipes update-cooked-with
                (:recipe/id recipe)
                (:cooked-with/ingredient c)
                :cooked-with/unit
                (-> e .-target .-value)))}]]
    [:td [:button {:on-click (fn [] (prn (:cooked-with/ingredient c)))}
          (:ingredient/name (id->ingredient (:cooked-with/ingredient c)))]]
    [:td (:recipe/name recipe)]
    [:td [:button {:on-click (fn [] (prn c))} "Print"]]
    [:td [:button {:on-click (fn [] (swap! !recipe-updates conj
                                          [[(:recipe/id recipe) (:cooked-with/ingredient c)]
                                           (dissoc c :cooked-with/amount-desc)]))} "Save"]]])

 (defn change-ingredient [c]
   (cond
     (and (is-ingredient? c "Milch")
          (= (:cooked-with/unit c) "Liter"))
     (assoc c :cooked-with/unit "l")
     (= (:cooked-with/ingredient c) (:ingredient/id (ingredient-name->ingredient "Tomate")))
     (assoc c :cooked-with/ingredient (:ingredient/id (ingredient-name->ingredient "Passierte Tomaten")))
     :else c))

 [:div.flex.flex-col.w-full
  [:table
   [:thead
    [:tr
     [:th "Amount"]
     [:th "Unit"]
     [:th "Ingredient"]
     [:th "Recipe"]]]
   [:tbody
    (->> @!recipes
         (mapcat (fn [r] (map
                         (fn [c] [c (dissoc r :recipe/cooked-with)])
                         (:recipe/cooked-with r))))
         (sort-by (comp :cooked-with/ingredient first))
         (map (fn [[c recipe]]
                [:<>
                 #_[cooked-with-component (-> c) recipe
                    "bg-gray-200"]
                 (when (not= (-> c
                                 fix-original
                                 fix-issue)
                             (-> c
                                 fix-original
                                 fix-issue
                                 change-unit
                                 change-ingredient))
                   [cooked-with-component (-> c
                                              fix-original
                                              fix-issue
                                              ) recipe
                    "bg-red-200"])
                 [cooked-with-component (-> c
                                            fix-original
                                            fix-issue
                                            change-unit
                                            change-ingredient) recipe
                  (when (not= (-> c
                                  fix-original
                                  fix-issue)
                              (-> c
                                  fix-original
                                  fix-issue
                                  change-unit
                                  change-ingredient))
                    "bg-green-200")]])))]]])
