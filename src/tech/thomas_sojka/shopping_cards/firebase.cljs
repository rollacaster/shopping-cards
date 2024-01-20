(ns tech.thomas-sojka.shopping-cards.firebase
  (:require ["firebase/app" :as firebase]))

(def firebaseConfig
  #js {:apiKey "AIzaSyAXNnWku5rgm33z6FtDoJu-IEEf1Y7RC1I"
       :authDomain "shopping-cards-e24af.firebaseapp.com"
       :projectId "shopping-cards-e24af"
       :storageBucket "shopping-cards-e24af.appspot.com"
       :messagingSenderId "235874496901"
       :appId "1:235874496901:web:dd7f231adee24e4b8f54ec"})

(def app (firebase/initializeApp firebaseConfig))
