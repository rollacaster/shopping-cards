(ns tech.thomas-sojka.shopping-cards.views.clean-amount-desc
  (:require [clojure.data :as data]
            [clojure.string :as str]
            [clojure.set :as set]
            [re-frame.core :refer [subscribe dispatch]]))

(defn index-by [f coll]
   (persistent! (reduce #(assoc! %1 (f %2) %2) (transient {}) coll)))

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

(def sizes
  {#{"EL" "TL"} "L"
   #{"ml" "l"} "l"
   #{"g" "kg"} "kg"})

(defn is-ingredient? [c ingredient]
   (= (:cooked-with/ingredient c)
      (:ingredient/id ingredient)))

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
     (assoc :cooked-with/unit (re-find #"[A-Za-zÄÜÖäüö]+" amount-desc))))

(defn fix-original [{:keys [cooked-with/amount cooked-with/amount-desc cooked-with/unit] :as c}]
   (cond-> c
     ;; no unit in amount-desc
     (and unit amount amount-desc (not (str/includes? amount-desc unit)))
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
     separate-amount-desc))

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
     (assoc :cooked-with/amount-desc (str/trim (str/replace amount-desc (:ingredient/name c) "")))

     ;; ingredient name in unit
     (and unit (str/includes? unit (:ingredient/name c)))
     (assoc :cooked-with/unit (str/trim (str/replace unit (:ingredient/name c) "")))))

(def remove-unit-and-amount #{"Kräuter" "Zitrone" "Salat" "Curry" "Salz" "Pfeffer"
                              "Kümmel" "Petersilie" "Currypaste" "Paprikapulver" "Garam Masala"
                              "Kurkuma" "Rosmarin" "Schnittlauch" "Thymian"
                              "Chili" "Basilikum"})
 (defn change-unit [c ingredient-name->ingredient]
   (cond
     (and (is-ingredient? c (ingredient-name->ingredient "Crème fraîche"))
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 20)))

     (and (is-ingredient? c (ingredient-name->ingredient "Kichererbsen"))
          (= (:cooked-with/unit c) "Dose"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 400)))

     (and (is-ingredient? c (ingredient-name->ingredient "Erbsen"))
          (= (:cooked-with/unit c) "Dose"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 400)))

     (and (is-ingredient? c (ingredient-name->ingredient "Nüsse"))
          (= (:cooked-with/unit c) "Stück"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Brühe"))
          (= (:cooked-with/unit c) "TL"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (update :cooked-with/amount #(* % 300)))

     (and (is-ingredient? c (ingredient-name->ingredient "Öl"))
          (= (:cooked-with/unit c) "ml"))
     (-> c
         (assoc :cooked-with/unit "EL")
         (update :cooked-with/amount #(/ % 15)))

     (and (is-ingredient? c (ingredient-name->ingredient "Passierte Tomaten"))
          (= (:cooked-with/unit c) "Packung"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 500)))

     (and (is-ingredient? c (ingredient-name->ingredient "Passierte Tomaten"))
          (= (:cooked-with/unit c) "ml"))
     (assoc c :cooked-with/unit "g")

     (and (is-ingredient? c (ingredient-name->ingredient "Milch"))
          (= (:cooked-with/unit c) "Liter"))
     (assoc c :cooked-with/unit "l")

     (and (is-ingredient? c (ingredient-name->ingredient "Reis"))
          (= (:cooked-with/unit c) "Pck."))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 200)))

     (and (is-ingredient? c (ingredient-name->ingredient "Gemüsebrühe"))
          (= (:cooked-with/unit c) "Tasse"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (update :cooked-with/amount #(* % 200)))

     (and (is-ingredient? c (ingredient-name->ingredient "Knoblauch"))
          (or (= (:cooked-with/unit c) "Zehe")
              (= (:cooked-with/unit c) "Stück")))
     (-> (assoc c :cooked-with/unit "Zehen"))

     (and (is-ingredient? c (ingredient-name->ingredient "Kokosmilch"))
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (update :cooked-with/amount #(* % 15)))

     (and (is-ingredient? c (ingredient-name->ingredient "Crème fraîche"))
          (= (:cooked-with/unit c) "ml"))
     (-> c
         (assoc :cooked-with/unit "g"))

     (and (is-ingredient? c (ingredient-name->ingredient "Tomatenmark"))
          (= (:cooked-with/unit c) "g"))
     (-> c
         (assoc :cooked-with/unit "EL")
         (update :cooked-with/amount (fn [amount] (max 1 (int (/ amount 25))))))

     (and (is-ingredient? c (ingredient-name->ingredient "Mehl"))
          (= (:cooked-with/unit c) "Tasse"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Gnocchi"))
          (= (:cooked-with/unit c) "Pck"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount (fn [amount] (* 500 amount))))

     (and (is-ingredient? c (ingredient-name->ingredient "Saure Sahne"))
          (nil? (:cooked-with/unit c)))
     (dissoc c :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Sahne"))
          (= (:cooked-with/unit c) "g"))
     (assoc c :cooked-with/unit "ml")

     (and (is-ingredient? c (ingredient-name->ingredient "Erdnussbutter"))
          (nil? (:cooked-with/unit c)))
     (-> c
         (assoc :cooked-with/unit "EL")
         (assoc :cooked-with/amount
                (js/parseFloat (re-find #"\d+" (:cooked-with/amount-desc c)))))

     (and (is-ingredient? c (ingredient-name->ingredient "Sahne"))
          (= (:cooked-with/unit c) "Becher"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (update :cooked-with/amount (fn [amount] (* 200 amount))))

     (and (is-ingredient? c (ingredient-name->ingredient "Mehl"))
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount (fn [amount] (* amount 10))))

     (and (is-ingredient? c (ingredient-name->ingredient "Bohnen"))
          (= (:cooked-with/unit c) "g"))
     (-> c
         (assoc :cooked-with/unit "Dose")
         (update :cooked-with/amount (fn [amount] (/ amount 500))))

     (and (is-ingredient? c (ingredient-name->ingredient "Mais"))
          (or (= (:cooked-with/unit c) "g")
              (nil? (:cooked-with/unit c)))
          (:cooked-with/amount c))
     (-> c
         (assoc :cooked-with/unit "Dose")
         (assoc :cooked-with/amount 1))

     (and (is-ingredient? c (ingredient-name->ingredient "Frühlingszwiebel"))
          (= (:cooked-with/unit c) "g"))
     (-> c
         (update :cooked-with/amount #(/ % 50))
         (dissoc :cooked-with/unit ))

     (is-ingredient? c (ingredient-name->ingredient "Frühlingszwiebel"))
     (dissoc c :cooked-with/unit )

     (and (is-ingredient? c (ingredient-name->ingredient "Schmand"))
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 15)))

     (and (is-ingredient? c (ingredient-name->ingredient "Parmesan"))
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 5)))

     (and (is-ingredient? c (ingredient-name->ingredient "Linsen"))
          (= (:cooked-with/unit c) "EL"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 15)))

     (and (is-ingredient? c (ingredient-name->ingredient "Parmesan"))
          (= (:cooked-with/unit c) "Handvoll"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Schmand"))
          (= (:cooked-with/unit c) "Becher"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 200)))

     (and (is-ingredient? c (ingredient-name->ingredient "Karotte"))
          (= (:cooked-with/unit c) "g"))
     (-> c
         (dissoc :cooked-with/unit)
         (update :cooked-with/amount #(js/Math.ceil (/ % 200))))

     (and (is-ingredient? c (ingredient-name->ingredient "Reis"))
          (= (:cooked-with/unit c) "Tasse"))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 150)))

     (and (is-ingredient? c (ingredient-name->ingredient "Butter"))
          (= (:cooked-with/unit c) "EL"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Semmel"))
          (= (:cooked-with/unit c) "g"))
     (-> c
         (dissoc :cooked-with/unit))

     (and (is-ingredient? c (ingredient-name->ingredient "Wasser"))
          (= (:cooked-with/unit c) "g"))
     (assoc c :cooked-with/unit "ml")

     (and (is-ingredient? c (ingredient-name->ingredient "Geriebener Käse"))
          (= (:cooked-with/unit c) "Packung"))
     (-> c
         (assoc :cooked-with/unit "g")
         (assoc :cooked-with/amount 250))

     (and (is-ingredient? c (ingredient-name->ingredient "Spinat"))
          (nil? (:cooked-with/unit c))
          (:cooked-with/amount c))
     (-> c
         (assoc :cooked-with/unit "g"))

     (and (is-ingredient? c (ingredient-name->ingredient "Wasser"))
          (= (:cooked-with/unit c) "Tasse"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (assoc :cooked-with/amount 250))

     (and (is-ingredient? c (ingredient-name->ingredient "Kokosmilch"))
          (= (:cooked-with/unit c) "Dose"))
     (-> c
         (assoc :cooked-with/unit "ml")
         (update :cooked-with/amount (fn [amount] (int (* amount 400)))))

     (and (is-ingredient? c (ingredient-name->ingredient "Zucker"))
          (= (:cooked-with/unit c) "Prise"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Hefe"))
          (= (:cooked-with/unit c) "g"))
     (dissoc c :cooked-with/unit :cooked-with/amount)

     (and (is-ingredient? c (ingredient-name->ingredient "Griechischer Joghurt"))
          (nil? (:cooked-with/unit c)))
     (assoc c :cooked-with/unit "g" :cooked-with/amount "200")

     (is-ingredient? c (ingredient-name->ingredient "Nudeln"))
     (-> c
         (assoc :cooked-with/unit "g"))

     (is-ingredient? c (ingredient-name->ingredient "Mozzarella"))
     (-> c
         (assoc :cooked-with/unit "Packung")
         (assoc :cooked-with/amount 1))

     (is-ingredient? c (ingredient-name->ingredient "Kräuter-Frischkäse"))
     (-> c
         (assoc :cooked-with/unit "Packung")
         (assoc :cooked-with/amount 1))

     (and (is-ingredient? c (ingredient-name->ingredient "Paprika"))
          (= (:cooked-with/unit c) "Stück"))
     (dissoc c :cooked-with/unit)

     (and (is-ingredient? c (ingredient-name->ingredient "Toast"))
          (= (:cooked-with/unit c) "Stk"))
     (dissoc c :cooked-with/unit)

     (and (is-ingredient? c (ingredient-name->ingredient "Zwiebel"))
          (= (:cooked-with/unit c) "Stück"))
     (dissoc c :cooked-with/unit)

     (is-ingredient? c (ingredient-name->ingredient "Eier"))
     (-> (dissoc c :cooked-with/unit)
         (update :cooked-with/amount js/Math.ceil))

     (and (is-ingredient? c (ingredient-name->ingredient "Kartoffeln"))
          (nil? (:cooked-with/unit c))
          (number? (:cooked-with/amount c)))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 200)))

     (and (is-ingredient? c (ingredient-name->ingredient "Mandeln"))
          (= (:cooked-with/unit c) "Handvoll"))
     (-> c
         (assoc :cooked-with/unit "g")
         (assoc :cooked-with/amount 25))

     (and (is-ingredient? c (ingredient-name->ingredient "Feta"))
          (nil? (:cooked-with/unit c))
          (number? (:cooked-with/amount c)))
     (-> c
         (assoc :cooked-with/unit "g")
         (update :cooked-with/amount #(* % 200)))

     (some #(is-ingredient? c (ingredient-name->ingredient %))
           remove-unit-and-amount)
     (dissoc c :cooked-with/unit :cooked-with/amount)

     :else c))

(defn- cooked-with-component [c recipe class id->ingredient]
   [:tr.px-2.w-16 {:class class}
    [:td
     (:cooked-with/amount c)]
    [:td.px-2.w-16
     (:cooked-with/unit c)]
    [:td [:button {:on-click (fn [] (prn (:cooked-with/ingredient c)))}
          (:ingredient/name (id->ingredient (:cooked-with/ingredient c)))]]
    [:td (:recipe/name recipe)]
    [:td [:button {:on-click (fn [] (prn c))} "Print"]]])

(defn- cooked-with-component2 [c recipe class id->ingredient children]
   [:tr.px-2.w-16 {:class class}
    [:td
     (:cooked-with/amount c)]
    [:td.px-2.w-16
     (:cooked-with/unit c)]
    [:td [:button {:on-click (fn [] (prn c))}
          (:ingredient/name (id->ingredient (:cooked-with/ingredient c)))]]
    [:td children]])

(defn change-ingredient [c ingredient-name->ingredient]
   (cond
     (and (is-ingredient? c "Milch")
          (= (:cooked-with/unit c) "Liter"))
     (assoc c :cooked-with/unit "l")
     (= (:cooked-with/ingredient c) (:ingredient/id (ingredient-name->ingredient "Tomate")))
     (assoc c :cooked-with/ingredient (:ingredient/id (ingredient-name->ingredient "Passierte Tomaten")))
     :else c))
(defn clean-cooked-with [c ingredient-name->ingredient]
  (-> c
      fix-original
      fix-issue
      (change-ingredient ingredient-name->ingredient)
      (change-unit ingredient-name->ingredient)))


(defn main []
  (let [recipes @(subscribe [:recipes])
        ingredients @(subscribe [:ingredients])
        id->ingredient (index-by :ingredient/id ingredients)
        ingredient-name->ingredient (index-by :ingredient/name ingredients)]
    [:<>
     [:div
      (->> @(subscribe [:recipes])
           (mapcat (fn [r]
                     (map
                      (fn [c]
                        (merge c (dissoc r :recipe/cooked-with)))
                      (:recipe/cooked-with r))))
           (group-by :ingredient/name)
           (keep (fn [[name cooked-with]]

                   (let [units (set (map #(some (fn [[sizes base-unit]] (when (sizes %) base-unit)) sizes) (map :cooked-with/unit cooked-with)))]
                     (when (and (> (count units) 1)
                                (not (contains? units nil)))
                       ^{:key name}
                       [:div.mb4
                        [:h1.b name]
                        [:table
                         [:thead
                          [:tr
                           [:th "Unit"]
                           [:th "Amount"]
                           [:th "Recipe"]]]
                         [:tbody
                          (map-indexed
                           (fn [i [unit cooked-with]]
                             ^{:key i}
                             [:tr
                              [:td  (or unit "none")]
                              [:td (count cooked-with)]
                              [:td (first (map :recipe/name cooked-with))]])
                           (group-by :cooked-with/unit cooked-with))]]])))))]
     [:div
      (->> @(subscribe [:recipes])
           (map (fn [r]
                  ^{:key (:recipe/id r)}
                  [:div

                   [:a {:href (:recipe/link r)} [:h1.mb3 (:recipe/name r)]]
                   [:div.flex.mb5
                    [:table.mr3
                     [:thead
                      [:tr
                       [:th "Amount"]
                       [:th "Unit"]
                       [:th "Ingredient"]]]
                     [:tbody
                      (map
                       (fn [c]
                         ^{:key (str (:recipe/id r) (:cooked-with/ingredient c))}
                         [cooked-with-component2 c r nil id->ingredient])
                       (:recipe/cooked-with r))]]
                    [:table
                     [:thead
                      [:tr
                       [:th "Amount"]
                       [:th "Unit"]
                       [:th "Ingredient"]]]
                     [:tbody
                      (map
                       (fn [c]
                         (let [cleaned-c (dissoc
                                          (clean-cooked-with c ingredient-name->ingredient)
                                          :cooked-with/amount-desc)
                               c (dissoc c :cooked-with/amount-desc)]
                           ^{:key (str (:recipe/id r) (:cooked-with/ingredient c))}
                           [:<>
                            [cooked-with-component2
                             cleaned-c
                             r
                             (when (not= c cleaned-c) "bg-green-200")
                             id->ingredient
                             (when (not= c cleaned-c)
                               [:button.bg-orange-700.white
                                {:on-click #(prn (data/diff
                                                  c
                                                  cleaned-c))}
                                "Diff"])]]
                           ))
                       (:recipe/cooked-with r))]]
                    [:button.bg-orange-700.pa2.white
                     {:on-click #(dispatch [:recipes/update
                                            (update r
                                                    :recipe/cooked-with
                                                    (fn [cooked-with]
                                                      (mapv
                                                       (fn [c] (clean-cooked-with c ingredient-name->ingredient))
                                                       cooked-with)))])}
                     "Update"]]])))
      ]]
    #_[:div.flex.flex-col.w-full
       [:table
        [:thead
         [:tr
          [:th "Amount"]
          [:th "Unit"]
          [:th "Ingredient"]
          [:th "Recipe"]]]
        [:tbody
         (->> recipes
              (remove (fn [{:keys [deleted]}] deleted))
              (mapcat (fn [r] (map
                              (fn [c] [c (dissoc r :recipe/cooked-with)])
                              (:recipe/cooked-with r))))
              (sort-by (comp :cooked-with/ingredient first))
              (map (fn [[c recipe]]
                     ^{:key (str (:recipe/id recipe) (:cooked-with/ingredient c))}
                     [:<>
                      (when (not= (-> c
                                      fix-original
                                      fix-issue)
                                  (-> c
                                      fix-original
                                      fix-issue
                                      (change-unit ingredient-name->ingredient)
                                      (change-ingredient ingredient-name->ingredient)))
                        [cooked-with-component
                         (-> c fix-original fix-issue)
                         recipe
                         "bg-red-200"
                         id->ingredient])
                      [cooked-with-component (-> c
                                                 fix-original
                                                 fix-issue
                                                 (change-unit ingredient-name->ingredient)
                                                 (change-ingredient ingredient-name->ingredient)) recipe
                       (when (not= (-> c
                                       fix-original
                                       fix-issue)
                                   (-> c
                                       fix-original
                                       fix-issue
                                       (change-unit ingredient-name->ingredient)
                                       (change-ingredient ingredient-name->ingredient)))
                         "bg-green-200")
                       id->ingredient]])))]]]))
