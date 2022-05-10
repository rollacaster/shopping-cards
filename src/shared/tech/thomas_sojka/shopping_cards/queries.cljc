(ns tech.thomas-sojka.shopping-cards.queries)

(def load-ingredients-by-recipe-id
  '[:find
    (pull ?c [:cooked-with/id
              :cooked-with/unit
              :cooked-with/amount-desc
              :cooked-with/amount])
    (pull ?i [:ingredient/id
              :ingredient/name
              {:ingredient/category [:db/ident]}])
    :in $ [?recipe-id ...]
    :where
    [?r :recipe/id ?recipe-id]
    [?c :cooked-with/recipe ?r]
    [?c :cooked-with/ingredient ?i]
    [?i :ingredient/category ?ca]
    (not [?ca :db/ident :ingredient-category/backen])
    (not [?ca :db/ident :ingredient-category/gewürze])])

(def load-ingredients
  '[:find (pull ?i [[:ingredient/id]
                    [:ingredient/name]
                    [:ingredient/category]])
    :where
    [?i :ingredient/id ]])
