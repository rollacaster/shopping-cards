(ns tech.thomas-sojka.shopping-cards.shopping-items
  (:require [clojure.string :as str]
            [re-frame.core :refer [inject-cofx reg-event-fx reg-sub]]
            [tech.thomas-sojka.shopping-cards.meal-plans :as meal-plans]
            [vimsical.re-frame.cofx.inject :as inject]))

(def firestore-path "shopping-items")

(reg-event-fx :shopping-item/deselect-ingredients
 (fn []
   {:app/push-state [:route/deselect-ingredients]
    :app/scroll-to [0 0]}))

(reg-event-fx :shopping-item/create
  [(inject-cofx ::inject/sub [:meals-without-shopping-list])]
  (fn [{:keys [meals-without-shopping-list]} [_ {:keys [items selected-items]}]]
    {:firestore/add-docs {:path firestore-path
                          :id :shopping-item/id
                          :data (->> items
                                     (filter (fn [[ingredient-id]] (selected-items ingredient-id)))
                                     (map (fn [[ingredient-id text]]
                                            {:shopping-item/ingredient-id ingredient-id
                                             :shopping-item/id (str (random-uuid))
                                             :shopping-item/content text
                                             :shopping-item/status :open
                                             :shopping-item/created-at (js/Date.)})))
                          :spec :shopping-item/shopping-entries
                          :on-success [:shopping-item/create-success]
                          :on-failure [:shopping-item/create-failure]}
     :firestore/update-docs {:path meal-plans/firestore-path
                             :id :id
                             :data (map #(assoc % :shopping-list true) meals-without-shopping-list)
                             :spec :meal-plan/meals}}))

(reg-event-fx :shopping-item/add
  (fn [_ [_ {:keys [ingredient-id content]}]]
    (let [new-id (str (random-uuid))]
      {:firestore/add-doc {:path firestore-path
                           :data {:shopping-item/id new-id
                                  :shopping-item/ingredient-id ingredient-id
                                  :shopping-item/content content
                                  :shopping-item/status :open
                                  :shopping-item/created-at (js/Date.)}
                           :key new-id
                           :spec :shopping-item/shopping-entry
                           :on-success [:shopping-item/add-success]
                           :on-failure [:shopping-item/add-failure]}})))

(reg-event-fx :shopping-item/add-success
 (fn []
   {:app/push-state [:route/shoppping-list]}))

(reg-event-fx :shopping-item/add-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-item/create-success
 (fn []
   {:app/push-state [:route/shoppping-list]}))

(reg-event-fx :shopping-item/create-failure
  (fn [{:keys [db]}]
    {:db (assoc db :app/error "Fehler: Speichern fehlgeschlagen")
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-item/load
  (fn []
    {:firestore/snapshot {:path firestore-path
                          :on-success [:shopping-item/load-success]
                          :on-failure [:shopping-item/load-failure]}}))

(defn- ->shopping-entry [firestore-shopping-entry]
  (-> firestore-shopping-entry
      (update :shopping-item/status keyword)
      (update :shopping-item/created-at (fn [date] (.toDate date)))))

(reg-event-fx :shopping-item/load-success
  (fn [{:keys [db]} [_ data]]
    {:db (assoc db :shopping-entries (map ->shopping-entry data))}
    ;; FIXME: Why did I add this archive check here?
    #_(cond-> {:db (assoc db :shopping-entries (map ->shopping-entry data))}
      (empty? (:shopping-entries db)) (assoc :dispatch [:shopping-items/archive]))))

(reg-event-fx :shopping-item/load-failure
  (fn [{:keys [db]}]
    {:db
     (assoc db
            :app/error "Fehler: Essen nicht geladen."
            :shopping-cards [])
     :app/timeout {:id :app/error-removal
                   :event [:app/remove-error]
                   :time 2000}}))

(reg-event-fx :shopping-item/update
  (fn [_ [_ {:shopping-item/keys [id] :as entry}]]
    {:firestore/update-doc {:path firestore-path
                            :key id
                            :data entry
                            :spec :shopping-item/shopping-entry}}))

(def penny-order ["05da97fa-3bc1-40b5-a5bb-d1d341d96b44" "4caae747-2b97-4d1a-b477-629781beae3f" "b287887d-346c-4644-ab84-4abc3eec81da" "8ce0dca4-db23-4ec8-a577-54e2caeeb802" "1ae57296-0493-4ac6-826e-549e4f4439a9" "2f989e52-c965-4f3c-af5b-e41ccd8b185f" "fa465e7b-c158-40b5-8d16-d2a156c476c6" "ffed7113-6388-4473-ad82-60aaf0ac00f9" "c6f1817a-9536-4747-9160-fd263e53564f" "3bc4e5ec-8cff-4ff4-b080-50a2a3aed6ec" "be78e544-68c8-4a06-89ba-6def6d88152d" "cfc2741b-c361-4d05-b71e-a2a118881400" "ef71e8f8-d318-41da-9421-41f1b39c8e1a" "49e653d8-08da-42c9-b19b-34320d400007" "7f47b604-d812-49f5-86f2-bbe41e629fde" "e24ed0cd-c1a4-4e7c-92a1-84bcda090efa" "6c0740a2-24a0-4aa7-9548-60c79bac6fec" "960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9" "8b77e903-6c87-4e48-b902-7035ca008374" "dc1b7bdc-9f9e-4751-b935-468919d39030" "da6b1d1d-08d1-4e5e-b086-031153d4033a" "a5b567e7-d639-4f64-a915-d9c7c59ac264" "3dfc35ac-47a8-4985-80db-39ebdc383cb2" "250b6a95-2dcc-4c23-b20c-c63cdec9c21f" "7e4b7ae1-d17b-4a51-bbca-3d5e45601fa8" "d4ac1410-3016-4e7a-8615-8c3b1a74c6ff" "38db3fcc-b62b-4b0c-bd58-d40430117f05" "45b8862f-6f2c-4a65-9245-552d69461235" "9f8d0bd9-4274-4f35-83fb-7916c43c77c3" "cd3243c4-09a0-472d-970f-dc912ce74610" "3e99ba7a-be59-4f98-98a1-2ad17b01d14e" "cfef02e2-13e8-40a1-8ea3-ac55b755e0e0" "f8e191a6-a0cc-4b75-82ec-b8a67e978f95" "aea1b07a-be01-45fa-8a7b-b1ee7b2fca16" "3c4bfe45-4c2a-4245-aa8f-2b8eb931e52e" "2402edca-bcdc-4ba0-a2a4-187568e9f7a1" "cec199c0-318a-475f-825c-4bbb7262778e" "4373e821-8697-48ee-becf-9776f7a4e794" "7cc3f4e2-fc7a-41d5-a2c8-65e53d9ad641" "175c3da0-a4c2-4bc4-ab37-2cfef0012ca2" "68da114e-5cd1-4c8b-b1e2-e24e325986a5" "3599b722-c387-433f-a20a-5ef1bcc0fd34" "5138699f-9d01-400e-97d8-3a02bf90c5b9" "030e4e1c-054d-4bb2-b208-c5cee01ca0e8" "109a3ce3-78c7-4d8a-ab33-2226050a41d8" "12e8f090-3da0-442c-9701-e34e19fd41e6" "1bdefb40-1eda-4f5a-9579-95fb75276bda" "282ef7bf-6d3a-4810-b331-5cf02d4e21b2" "2a74fe69-11c6-435c-8e57-2798243fabb2" "2b5e354e-6408-4b51-81a7-4cb7dfb6f39f" "308d3d71-cbaf-437b-9f14-2d059515843e" "43a6f610-b503-4116-9c0b-b540dd5a2778" "49f864fc-b05e-4136-90c2-28ad021a2c88" "539ae848-13bd-410d-9830-32822b02443c" "a4929cb9-8148-40c4-9875-c0860768b3b4" "e67009b0-be0f-4c3e-b23b-75ff752fa643" "2c78778a-2331-4d42-b7bb-02cc759f17da" "4b7d675c-a303-4bda-9ead-6f216b86a814" "8997964c-df10-4e3c-8321-cc372a56ea94" "fcf62e6b-597a-4711-ad79-1caf84243061" "9edc6e43-a040-4829-88f8-de0eaa0a5209" "cb5515eb-e63f-4a05-9c9f-1570220c8f71" "e350c33d-a77a-45d5-b965-7e07b0f5588c" "fa805d56-ac9e-4975-8e1f-7615ad28b297" "1d89bbb9-3052-45f7-9be9-d7a0c54c9b45" "64e066ef-1e5b-4c67-aa34-acc6fee52ed8" "afb01109-0750-4a59-9e63-7ad4fbb891b5" "31bff67e-5285-475a-8d03-0228923d7215" "9597d110-d042-4203-990f-c339c2d61109" "539de6ed-3f2c-40b2-a05f-904a388844a8" "589c086e-b06b-4451-aba2-90c46b8c1702" "d06107aa-68b5-41f0-8256-f124e5d0f240" "4d3c25e5-198b-40a2-9aca-467e64185384" "5afade6b-f382-452c-91cb-edc761380983" "1880eb8a-dafd-4ea2-a985-7789bc99e0a2" "132de4f5-99cf-4078-9063-ff5b89d54aec" "95b94ddd-c86f-4502-b809-a9b050d28e5b" "bf5870c3-64f9-42c9-a584-8371dbb5358e" "4e67d72f-44c4-464f-be33-05382c3c8080" "54c81443-15b7-48b9-883f-0d88cc131e5e" "94f58d5a-221b-48d4-9f9e-118d1fdce128" "9580eac9-5902-4420-917d-7d6539c64c9b" "8c98a110-b7cd-477c-ab8f-9a08074ab779" "1f5331de-5ab4-4636-b33d-4812459c063c" "cfccfc5d-4977-4eda-8716-0fc1e71944e8" "f966190f-50fd-41aa-a7fa-f55bb3ae6d61" "690fdb5c-711b-4b1b-918b-148d2a4eb355" "36071b41-2b7b-4431-aad1-8a87b99327f6" "d33ad997-4d02-4437-8d35-db8e22fdb4b0" "73233dd2-991a-40ea-8e3b-384bcf2ac408" "6a5fb199-bac0-4db1-bc2b-16d65c0ccfdb" "a1ab22e1-8f4b-411e-a9e9-a1c5efa6c51d" "15e8d610-ce9f-405b-957c-5f4fb6249b20" "f29557a1-d028-4efa-860d-562ad6fe8c56" "3654b906-0dac-4db9-bf25-e4fbb9f4439f" "2a647d9b-4a02-4853-bad4-ca0f9201ed8b" "b34db180-ff4f-45f6-9e06-4b20d58323e9" "c181aef6-0e09-43c1-85d1-2aaabdb1ce6a" "5281f0e4-cc6f-4a2b-91c2-a9c212919134" "d71c24e8-85d4-4dc3-bb0d-1535a537fcf4" "b98dbaa1-6112-4f57-a879-5d58ed24bf91" "a63da408-9142-43aa-9ffa-1eb6b095c0bd" "ed57f4f9-d03b-49ce-8350-d9140918de2b" "f053c6cd-0fc0-481a-8779-bac970a8dd8c" "152afeb9-c1ca-4256-8ec3-a90d4f6e5f5e" "3c541633-5f65-452f-aca5-313fcec1be31" "6c2eae84-f52a-42b7-9448-199dbde69bb9" "66127547-25f2-4777-8c74-cad264c6a5e1" "bb9d1c96-9301-4fc9-9563-73a607fce3f6" "3dc8331b-e1bf-4b59-9883-96bb855f9dfd" "c649995f-c56f-40d9-bb65-b6e57d9c1d73" "5381790d-3485-47ff-92bd-90d03f1f62a8" "5ef93777-a340-4f21-b06f-1fce3dba2703" "e3a78734-79bd-4304-bd7c-f43e203ec8b1" "577755d6-91c8-4815-bfca-dea5e362300c" "b47c89f9-f959-48f1-a955-b2642253350e" "fdde7243-3e9e-4044-98bb-b7ba8a6414b1" "f5a0f2a7-efa8-4fa4-9ba1-d9dc402364df" "7485cc2e-6a74-430a-9273-659334fbec21" "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd" "99544496-9a6e-46c4-940b-694918db5071" "3382e500-33fc-4803-9bea-56ab17e3d044" "6964c9bf-bdae-45fc-8978-c5eb8d22a810" "0f552276-6d15-43db-95e6-0572a50602fe" "22697b74-e156-4472-9aae-0bd2740cf9b9" "f365f293-4adc-4cdc-bcca-389425f3e2e6" "81c4abf6-9c3c-421e-8f11-bb78f3cfeb54" "14a0c9c7-3630-4ea3-957f-3807cd624636" "9e0c19af-f27f-4b04-99fd-689357ee1be8" "ff2eb6cd-7d1a-4eb9-b1f6-35e94db085d7" "01cee208-21ad-4c6c-a9d7-900f3f8b0214" "1f6fd79e-fb13-47ee-86a6-e152d4a9c36c" "bcc4ce43-e3c0-4040-b629-30c1a247038f" "e03ed372-2314-4555-9313-44f942884528" "2ab8d134-6781-441a-a3d1-4ad6262cd4e3" "3870839e-2424-494d-954f-244db4341234" "648480b7-1e76-40c2-a88e-4291543146fe" "2ce6a405-ebd7-4bb7-b682-3f7c146971e3" "5d640387-65eb-4d07-86ec-97f4ad8e3c8b" "c31b04d7-8009-4a63-935e-6185b226280e" "b9e5b157-21f7-4064-9356-42944722aa8e" "e0f74a51-d1b7-46f5-b0ad-d5b64de3d24b" "6d52b4da-fa8f-4b13-aa1d-26a4a4e661cd" "e1e51479-47c2-4acd-858a-910c66549292" "c400daec-62a8-4802-9455-02a4e93eba78" "7308596e-b6c9-4f11-a59a-bf38364ebd48" "2afef478-85f4-4e5c-baeb-b04f48e4a945" "b6d355fd-af3e-4b23-8949-f11cb93f0570" "454cf883-2480-4c24-a8e8-c078c522abef" "a34e4012-c32d-4186-ae83-4c791938f1f1" "5b34388f-4b84-4242-8acc-0514a75e57b5" "bccaa355-9245-4d13-87fb-705267683f41" "d6ed05b5-a1a9-4c6f-8832-ddfb02344b83" "24de8be6-1300-41be-9814-f698aacf1638" "ae2892c0-9e51-4db8-a6ef-790e8b75b506" "64e38f58-0fa1-4dee-8f41-fbac25a77f5f" "24a03356-60cd-4f92-9f79-4cc511dd6d7e" "d8396502-884f-4314-9e47-42545fe4be84" "20add41c-8575-4ebc-813a-50830c51f699" "428d5bc6-9c07-4852-ad14-aefeb49f380e" "c1604163-078b-4ef8-be0d-9740eb2636f1" "d7d3faa8-f7c1-4cf2-ba0b-23df648b3c7c" "ea3e293f-a982-484b-bc0c-c5d27afae2f7" "b9bf8d43-15d0-4460-869e-d44b06cd815a" "8f485322-2016-4642-a8b8-47f5c10ff599" "e1f9c39b-1a3a-4512-a123-30e93c38d37f" "a0a33606-3241-44bc-b91e-c09f10dd6054" "cdfe6ae3-fbad-4c98-9f2f-e91347856399" "5316b1b4-73e3-4431-b12c-2740e13d18f3" "5df38cbe-cd05-4140-ade8-dae74db385b5" "e7691a24-6d24-401f-9516-1fc7cf33f880" "c0cf3e5e-609f-474e-9fde-810285a0c31b" "f907e5df-dbe9-48a2-bc13-70884b1e56f0" "ee34edf4-05af-481d-a3c1-46371349f942" "60ab2373-1dbd-4884-8a9b-5ae626d245ce" "864a7140-73c9-4360-801e-cf056886b7fb" "60e4faa4-b39a-4539-b516-cdeceffd5541" "2c570b4c-6cbf-4424-a3e0-5bf60ae47d51" "2dee32c2-1b68-4eea-a80d-5c5668a24d45" "d11984bc-acb6-4801-8e99-12ee8c648dfe" "6175d1a2-0af7-43fb-8a53-212af7b72c9c" "e6ce2fbe-8f6b-442e-a9e3-cdb67a1c90a1" "83d5b8e1-945f-4917-9cd7-f9ddb6330b89" "d337321c-7d05-4449-b7ec-0ed0a611cd0d"])

(defn- ingredient-text [cooked-with]
  (let [no-unit? (->> cooked-with
                      (map (fn [{:keys [cooked-with/unit]}] unit))
                      (every? nil?))
        amount-descs (map (fn [{:keys [cooked-with/amount-desc]}] amount-desc) cooked-with)
        amounts (map (fn [{:keys [cooked-with/amount]}] amount) cooked-with)
        no-amount? (every? nil? amount-descs)
        {:keys [:cooked-with/amount-desc]} (first cooked-with)
        {:keys [ingredient/name]} (first cooked-with)]
    (str/trim
     (cond no-amount? name
           (= (count cooked-with) 1) (str amount-desc " " name)
           (and no-unit? no-amount?) (str (float (reduce + amounts)) " " name)
           :else (str (count cooked-with)
                      " " name
                      " (" (str/join ", " amount-descs) ")")))))

(defn- sort-shopping-items [sort-order ingredients]
  (let [sort-order-map (into {} (map-indexed (fn [idx id] [id idx]) sort-order))]
    (sort-by (fn [[id _]] (sort-order-map id)) ingredients)))

(defn- no-shoppping-categories []
  #{:ingredient-category/backen :ingredient-category/gewürze})

(defn create-possible-shopping-items [sort-order meals-plans]
  (->> meals-plans
       (mapcat (comp :recipe/cooked-with :recipe))
       (remove (fn [{:keys [ingredient/category]}] ((no-shoppping-categories) category)))
       (group-by :ingredient/id)
       (sort-shopping-items sort-order)
       (map (fn [[ingredient-id ingredients]] [ingredient-id (ingredient-text ingredients)]))))

(reg-sub :shopping-item/possible-items
  :<- [:meals-without-shopping-list]
  (partial create-possible-shopping-items penny-order))

(reg-sub :shopping-entries
  (fn [db]
    (remove (fn [{:keys [shopping-item/status]}] (= status :archive))
            (:shopping-entries db))))

(reg-sub :shopping-entries?
  :<- [:shopping-entries]
  (fn [shopping-entries]
    (seq shopping-entries)))

(reg-event-fx :shopping-items/archive
  (fn [{{:keys [shopping-entries]} :db}]
    {:firestore/update-docs {:path firestore-path
                             :id :shopping-item/id
                             :data (->> shopping-entries
                                        (filter (fn [{:keys [shopping-item/status]}] (= status :done)))
                                        (map (fn [i] (assoc i :shopping-item/status :archive))))
                             :spec :shopping-item/shopping-entries}}))

;; All ingredients which are not already on the shopping-list
(reg-sub :shopping-item/possible-ingredients
  :<- [:ingredients]
  :<- [:shopping-entries]
  (fn [[ingredients shopping-entries]]
    (remove
     (fn [{:keys [ingredient/id]}]
       ((set (map :shopping-item/ingredient-id shopping-entries)) id))
     ingredients)))
