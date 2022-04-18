(ns tech.thomas-sojka.shopping-cards.components)

(def icons {:check-mark "M20.285 2l-11.285 11.567-5.286-5.011-3.714 3.716 9 8.728 15-15.285z"
            :trash-can "M3 6v18h18v-18h-18zm5 14c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm5 0c0 .552-.448 1-1 1s-1-.448-1-1v-10c0-.552.448-1 1-1s1 .448 1 1v10zm4-18v2h-20v-2h5.711c.9 0 1.631-1.099 1.631-2h5.315c0 .901.73 2 1.631 2h5.712z"
            :shopping-cart "m 18.595802,0.56054688 c -0.459356,0 -0.816128,0.30784707 -0.967618,0.71344852 L 13.552604,14.165318 H 4.8395597 L 2.3961775,6.9817746 h 9.1724185 c 0.561978,0 1.021353,-0.4593749 1.021353,-1.0213529 0,-0.561978 -0.459375,-1.021353 -1.021353,-1.021353 H 1.0180526 C 0.11400118,4.9830496 -0.11561948,5.8187628 0.05053051,6.2145907 L 3.1584936,15.484745 c 0.1514897,0.405601 0.5620161,0.664581 0.9676175,0.664581 H 14.266147 c 0.459356,0 0.864901,-0.254093 0.967523,-0.664581 L 19.30925,2.5934218 h 3.669941 c 0.561978,0 1.021353,-0.4592795 1.021353,-1.0212574 0,-0.5619779 -0.464262,-1.01161752 -1.021353,-1.01161752 z M 5.0447658,17.98673 c -1.7836692,0 -3.2594339,1.47586 -3.2594339,3.259529 0,1.783669 1.4757647,3.259434 3.2594339,3.259434 1.783669,0 3.259529,-1.475765 3.259529,-3.259434 0,-1.783669 -1.47586,-3.259529 -3.259529,-3.259529 z m 7.9458602,0 c -1.783669,0 -3.2594341,1.47586 -3.2594341,3.259529 0,1.783669 1.4757651,3.259434 3.2594341,3.259434 1.832536,0 3.259529,-1.475765 3.259529,-3.259434 0.0049,-1.783669 -1.47586,-3.259529 -3.259529,-3.259529 z M 5.0447658,20.0197 c 0.7134676,0.0049 1.2754455,0.561959 1.2216911,1.275427 0,0.713467 -0.5619779,1.275426 -1.2216911,1.275426 -0.6646002,0 -1.2216913,-0.561959 -1.2216913,-1.275426 0,-0.713468 0.5619779,-1.275427 1.2216913,-1.275427 z m 7.9508232,0 c 0.708581,0.0049 1.270558,0.561959 1.221691,1.275427 0,0.713467 -0.561978,1.275426 -1.221691,1.275426 -0.659713,0 -1.221691,-0.561959 -1.221691,-1.275426 0,-0.713468 0.561978,-1.275427 1.221691,-1.275427 z"
            :add "M12 2c5.514 0 10 4.486 10 10s-4.486 10-10 10-10-4.486-10-10 4.486-10 10-10zm0-2c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm6 13h-5v5h-2v-5h-5v-2h5v-5h2v5h5v2z"
            :remove "M12 2c5.514 0 10 4.486 10 10s-4.486 10-10 10-10-4.486-10-10 4.486-10 10-10zm0-2c-6.627 0-12 5.373-12 12s5.373 12 12 12 12-5.373 12-12-5.373-12-12-12zm6 13h-12v-2h12v2z"
            :back "M16.67 0l2.83 2.829-9.339 9.175 9.339 9.167-2.83 2.829-12.17-11.996z"})

(defn icon
  ([name]
   [icon {} name])
  ([{:keys [class style]} name]
   [:svg {:viewBox "0 0 24 24" :class class :style style}
    [:path {:d (icons name) :fill "currentColor"}]]))

(defn recipe [{:keys [even name image selected? on-click]}]
  [:button.relative.w-100.w-auto-ns.flex.db-ns.tl.outline-transparent.bg-trbg-gray-600-ns.white-ns.pa0.bt-0.br-0.bl-0.bb-0-ns.bb.b--gray-900.bw1.ml3-ns.mb3-ns.br2-ns.h3.h-auto-ns
   {:on-click on-click :class (if even "bg-gray-600 white" "bg-gray-300")}
   [:div.w5-ns.h5-ns.h-100.shadow-3-ns.w-20.z-1
    (when selected?
      [:<>
       [:div.w3.h3.absolute.orange-400.db.dn-ns
        {:style {:top "50%" :left "2%" :transform "translate(0,-50%)"}}
        [icon :check-mark]]
       [:div.w4.h4.absolute.white.dn.db-ns
        {:style {:top "50%" :left "50%" :transform "translate(-50%,-50%)"}}
        [icon :check-mark]]])
    [:img.br2-ns.w-100.h-100 {:style {:object-fit "cover"} :src image :class (when selected? "o-40")}]]
   [:div.bg-gray-700.absolute.pa2.mh2.mb2.bottom-0.o-50.br2.dn.db-ns
    [:span.f4 name]]
   [:div.absolute-ns.pa2.mb2-ns.mh2.bottom-0.w-80.w-auto-ns
    {:class (when selected? "o-40")}
    [:span.f4 name]]])

(defn recipe-details [{:keys [name link on-remove image ingredients]}]
  [:div.ph5-ns.ph3.pv4.ml2-ns.bg-gray-200
   [:div.flex.justify-between.items
    [:a.link.near-black.mb3.mb0-ns.db
     {:href link :target "_blank" :referer "norel noopener"
      :class (when-not (empty? link) "underline")}
     [:h1.mv0 name]]
    [:button.pv2.br3.bg-orange-200.bn.shadow-2.self-start
     {:on-click on-remove}
     [icon {:class "dark-gray h2"} :trash-can]]]
   [:div.flex.justify-between.flex-wrap
    [:div.bw1.w-50-ns.order-1-ns.flex.justify-center-ns.h-100
     [:img.w5.br3.ba.b--orange-300 {:src image}]]
    [:ul.pl0.list.mb4.w-100.w-50-ns.order-0-ns
     (map
      (fn [[id ingredient]]
        [:li.mb3.f4 {:key id} ingredient])
      ingredients)]]
   (when-not (empty? link)
     [:div.flex.justify-center
      [:a.link.shadow-3.bn.pv2.ph3.br2.bg-orange-400.f3.gray-800
       {:href link}
       "Rezept anzeigen"]])])
