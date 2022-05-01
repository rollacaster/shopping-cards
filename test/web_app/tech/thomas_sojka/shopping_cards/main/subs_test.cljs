(ns tech.thomas-sojka.shopping-cards.main.subs-test
  (:require [tech.thomas-sojka.shopping-cards.main.subs :as sut]
            [cljs.test :as t :include-macros true]))

(def recipes [{:id "501c77c8-498b-4bec-a2fa-6ea1837eae6b",
               :name "Gemüse-Lassange",
               :image
               "https://img.chefkoch-cdn.de/rezepte/2114131340630587/bilder/1251192/crop-360x240/vegetarische-spinat-gemuese-lasagne-mit-tomatensosse.jpg",
               :link
               "https://www.chefkoch.de/rezepte/1143511220703880/Julies-feine-Gemueselasagne.html",
               :type :recipe-type/normal}
              {:id "f05e0d11-a18c-466f-a83a-cfab622f57c9",
               :name "Spinat-Reis mit Blumenkohl mit Käse überbacken",
               :image
               "https://img.chefkoch-cdn.de/rezepte/159921069926870/bilder/544569/crop-360x240/reis-mit-spinat-und-schafskaese.jpg",
               :link
               "https://docs.google.com/document/d/1EF81XXK9o6YBA0oq83maiUJ2RkmToh-FO0JUVtw5528/edit",
               :type :recipe-type/fast}
              {:id "b43b5d14-45ee-4877-bf8c-fe539134e315",
               :name "Penne con verdura",
               :image
               "https://img.chefkoch-cdn.de/rezepte/3346661497357496/bilder/1199324/crop-360x240/penne-verdura-dalla-padella.jpg",
               :link "https://www.eat-this.org/penne-con-verdura/",
               :type :recipe-type/rare}])

(t/deftest subscriptions
  (t/testing "sorted-recipes"
    (t/is
     (=
      (sut/sorted-recipes recipes)
      [[:recipe-type/normal
        [{:id "501c77c8-498b-4bec-a2fa-6ea1837eae6b",
          :name "Gemüse-Lassange",
          :image
          "https://img.chefkoch-cdn.de/rezepte/2114131340630587/bilder/1251192/crop-360x240/vegetarische-spinat-gemuese-lasagne-mit-tomatensosse.jpg",
          :link
          "https://www.chefkoch.de/rezepte/1143511220703880/Julies-feine-Gemueselasagne.html",
          :type :recipe-type/normal}]]
       [:recipe-type/fast
        [{:id "f05e0d11-a18c-466f-a83a-cfab622f57c9",
          :name "Spinat-Reis mit Blumenkohl mit Käse überbacken",
          :image
          "https://img.chefkoch-cdn.de/rezepte/159921069926870/bilder/544569/crop-360x240/reis-mit-spinat-und-schafskaese.jpg",
          :link
          "https://docs.google.com/document/d/1EF81XXK9o6YBA0oq83maiUJ2RkmToh-FO0JUVtw5528/edit",
          :type :recipe-type/fast}]]
       [:recipe-type/rare
        [{:id "b43b5d14-45ee-4877-bf8c-fe539134e315",
          :name "Penne con verdura",
          :image
          "https://img.chefkoch-cdn.de/rezepte/3346661497357496/bilder/1199324/crop-360x240/penne-verdura-dalla-padella.jpg",
          :link "https://www.eat-this.org/penne-con-verdura/",
          :type :recipe-type/rare}]]])))

  (t/testing "lunch-recipes"
    (t/is
     (=
      (sut/lunch-recipes recipes)
      [[:recipe-type/fast
        [{:id "f05e0d11-a18c-466f-a83a-cfab622f57c9",
          :name "Spinat-Reis mit Blumenkohl mit Käse überbacken",
          :image
          "https://img.chefkoch-cdn.de/rezepte/159921069926870/bilder/544569/crop-360x240/reis-mit-spinat-und-schafskaese.jpg",
          :link
          "https://docs.google.com/document/d/1EF81XXK9o6YBA0oq83maiUJ2RkmToh-FO0JUVtw5528/edit",
          :type :recipe-type/fast}]]
       [:recipe-type/normal
        [{:id "501c77c8-498b-4bec-a2fa-6ea1837eae6b",
          :name "Gemüse-Lassange",
          :image
          "https://img.chefkoch-cdn.de/rezepte/2114131340630587/bilder/1251192/crop-360x240/vegetarische-spinat-gemuese-lasagne-mit-tomatensosse.jpg",
          :link
          "https://www.chefkoch.de/rezepte/1143511220703880/Julies-feine-Gemueselasagne.html",
          :type :recipe-type/normal}]]
       [:recipe-type/rare
        [{:id "b43b5d14-45ee-4877-bf8c-fe539134e315",
          :name "Penne con verdura",
          :image
          "https://img.chefkoch-cdn.de/rezepte/3346661497357496/bilder/1199324/crop-360x240/penne-verdura-dalla-padella.jpg",
          :link "https://www.eat-this.org/penne-con-verdura/",
          :type :recipe-type/rare}]]]))))
