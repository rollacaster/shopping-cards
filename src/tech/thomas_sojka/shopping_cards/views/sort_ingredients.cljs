(ns tech.thomas-sojka.shopping-cards.views.sort-ingredients
  (:require [re-frame.core :as rf]
            ["react-beautiful-dnd" :as react-beautiful-dnd]
            [reagent.core :as r]
            ["react-tooltip" :as react-tooltip]))
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

(def penny-category-order
  [:ingredient-category/obst
   :ingredient-category/gemüse
   :ingredient-category/gewürze
   :ingredient-category/tiefkühl
   :ingredient-category/brot&co
   :ingredient-category/müsli&co
   :ingredient-category/konserven
   :ingredient-category/beilage
   :ingredient-category/backen
   :ingredient-category/kosmetik
   :ingredient-category/fleisch
   :ingredient-category/wursttheke
   :ingredient-category/milch&co
   :ingredient-category/käse&co
   :ingredient-category/süßigkeiten
   :ingredient-category/eier
   :ingredient-category/getränke])

(def kitchen-category-order
  [:ingredient-category/käse&co
   :ingredient-category/milch&co
   :ingredient-category/eier
   :ingredient-category/gemüse
   :ingredient-category/tiefkühl
   :ingredient-category/beilage
   :ingredient-category/obst
   :ingredient-category/konserven
   :ingredient-category/backen
   :ingredient-category/gewürze
   :ingredient-category/brot&co
   :ingredient-category/müsli&co
   :ingredient-category/kosmetik
   :ingredient-category/süßigkeiten
   :ingredient-category/getränke
   :ingredient-category/fleisch
   :ingredient-category/wursttheke])
(def penny-order ["05da97fa-3bc1-40b5-a5bb-d1d341d96b44" "4caae747-2b97-4d1a-b477-629781beae3f" "b287887d-346c-4644-ab84-4abc3eec81da" "8ce0dca4-db23-4ec8-a577-54e2caeeb802" "1ae57296-0493-4ac6-826e-549e4f4439a9" "2f989e52-c965-4f3c-af5b-e41ccd8b185f" "fa465e7b-c158-40b5-8d16-d2a156c476c6" "ffed7113-6388-4473-ad82-60aaf0ac00f9" "c6f1817a-9536-4747-9160-fd263e53564f" "3bc4e5ec-8cff-4ff4-b080-50a2a3aed6ec" "be78e544-68c8-4a06-89ba-6def6d88152d" "cfc2741b-c361-4d05-b71e-a2a118881400" "ef71e8f8-d318-41da-9421-41f1b39c8e1a" "49e653d8-08da-42c9-b19b-34320d400007" "7f47b604-d812-49f5-86f2-bbe41e629fde" "e24ed0cd-c1a4-4e7c-92a1-84bcda090efa" "6c0740a2-24a0-4aa7-9548-60c79bac6fec" "960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9" "8b77e903-6c87-4e48-b902-7035ca008374" "dc1b7bdc-9f9e-4751-b935-468919d39030" "da6b1d1d-08d1-4e5e-b086-031153d4033a" "a5b567e7-d639-4f64-a915-d9c7c59ac264" "3dfc35ac-47a8-4985-80db-39ebdc383cb2" "250b6a95-2dcc-4c23-b20c-c63cdec9c21f" "7e4b7ae1-d17b-4a51-bbca-3d5e45601fa8" "d4ac1410-3016-4e7a-8615-8c3b1a74c6ff" "38db3fcc-b62b-4b0c-bd58-d40430117f05" "45b8862f-6f2c-4a65-9245-552d69461235" "9f8d0bd9-4274-4f35-83fb-7916c43c77c3" "cd3243c4-09a0-472d-970f-dc912ce74610" "3e99ba7a-be59-4f98-98a1-2ad17b01d14e" "cfef02e2-13e8-40a1-8ea3-ac55b755e0e0" "f8e191a6-a0cc-4b75-82ec-b8a67e978f95" "aea1b07a-be01-45fa-8a7b-b1ee7b2fca16" "3c4bfe45-4c2a-4245-aa8f-2b8eb931e52e" "2402edca-bcdc-4ba0-a2a4-187568e9f7a1" "cec199c0-318a-475f-825c-4bbb7262778e" "4373e821-8697-48ee-becf-9776f7a4e794" "7cc3f4e2-fc7a-41d5-a2c8-65e53d9ad641" "175c3da0-a4c2-4bc4-ab37-2cfef0012ca2" "68da114e-5cd1-4c8b-b1e2-e24e325986a5" "3599b722-c387-433f-a20a-5ef1bcc0fd34" "5138699f-9d01-400e-97d8-3a02bf90c5b9" "030e4e1c-054d-4bb2-b208-c5cee01ca0e8" "109a3ce3-78c7-4d8a-ab33-2226050a41d8" "12e8f090-3da0-442c-9701-e34e19fd41e6" "1bdefb40-1eda-4f5a-9579-95fb75276bda" "282ef7bf-6d3a-4810-b331-5cf02d4e21b2" "2a74fe69-11c6-435c-8e57-2798243fabb2" "2b5e354e-6408-4b51-81a7-4cb7dfb6f39f" "308d3d71-cbaf-437b-9f14-2d059515843e" "43a6f610-b503-4116-9c0b-b540dd5a2778" "49f864fc-b05e-4136-90c2-28ad021a2c88" "539ae848-13bd-410d-9830-32822b02443c" "a4929cb9-8148-40c4-9875-c0860768b3b4" "e67009b0-be0f-4c3e-b23b-75ff752fa643" "2c78778a-2331-4d42-b7bb-02cc759f17da" "4b7d675c-a303-4bda-9ead-6f216b86a814" "8997964c-df10-4e3c-8321-cc372a56ea94" "fcf62e6b-597a-4711-ad79-1caf84243061" "9edc6e43-a040-4829-88f8-de0eaa0a5209" "cb5515eb-e63f-4a05-9c9f-1570220c8f71" "e350c33d-a77a-45d5-b965-7e07b0f5588c" "fa805d56-ac9e-4975-8e1f-7615ad28b297" "1d89bbb9-3052-45f7-9be9-d7a0c54c9b45" "64e066ef-1e5b-4c67-aa34-acc6fee52ed8" "afb01109-0750-4a59-9e63-7ad4fbb891b5" "31bff67e-5285-475a-8d03-0228923d7215" "9597d110-d042-4203-990f-c339c2d61109" "539de6ed-3f2c-40b2-a05f-904a388844a8" "589c086e-b06b-4451-aba2-90c46b8c1702" "d06107aa-68b5-41f0-8256-f124e5d0f240" "4d3c25e5-198b-40a2-9aca-467e64185384" "5afade6b-f382-452c-91cb-edc761380983" "1880eb8a-dafd-4ea2-a985-7789bc99e0a2" "132de4f5-99cf-4078-9063-ff5b89d54aec" "95b94ddd-c86f-4502-b809-a9b050d28e5b" "bf5870c3-64f9-42c9-a584-8371dbb5358e" "4e67d72f-44c4-464f-be33-05382c3c8080" "54c81443-15b7-48b9-883f-0d88cc131e5e" "94f58d5a-221b-48d4-9f9e-118d1fdce128" "9580eac9-5902-4420-917d-7d6539c64c9b" "8c98a110-b7cd-477c-ab8f-9a08074ab779" "1f5331de-5ab4-4636-b33d-4812459c063c" "cfccfc5d-4977-4eda-8716-0fc1e71944e8" "f966190f-50fd-41aa-a7fa-f55bb3ae6d61" "690fdb5c-711b-4b1b-918b-148d2a4eb355" "36071b41-2b7b-4431-aad1-8a87b99327f6" "d33ad997-4d02-4437-8d35-db8e22fdb4b0" "73233dd2-991a-40ea-8e3b-384bcf2ac408" "6a5fb199-bac0-4db1-bc2b-16d65c0ccfdb" "a1ab22e1-8f4b-411e-a9e9-a1c5efa6c51d" "15e8d610-ce9f-405b-957c-5f4fb6249b20" "f29557a1-d028-4efa-860d-562ad6fe8c56" "3654b906-0dac-4db9-bf25-e4fbb9f4439f" "2a647d9b-4a02-4853-bad4-ca0f9201ed8b" "b34db180-ff4f-45f6-9e06-4b20d58323e9" "c181aef6-0e09-43c1-85d1-2aaabdb1ce6a" "5281f0e4-cc6f-4a2b-91c2-a9c212919134" "d71c24e8-85d4-4dc3-bb0d-1535a537fcf4" "b98dbaa1-6112-4f57-a879-5d58ed24bf91" "a63da408-9142-43aa-9ffa-1eb6b095c0bd" "ed57f4f9-d03b-49ce-8350-d9140918de2b" "f053c6cd-0fc0-481a-8779-bac970a8dd8c" "152afeb9-c1ca-4256-8ec3-a90d4f6e5f5e" "3c541633-5f65-452f-aca5-313fcec1be31" "6c2eae84-f52a-42b7-9448-199dbde69bb9" "66127547-25f2-4777-8c74-cad264c6a5e1" "bb9d1c96-9301-4fc9-9563-73a607fce3f6" "3dc8331b-e1bf-4b59-9883-96bb855f9dfd" "c649995f-c56f-40d9-bb65-b6e57d9c1d73" "5381790d-3485-47ff-92bd-90d03f1f62a8" "5ef93777-a340-4f21-b06f-1fce3dba2703" "e3a78734-79bd-4304-bd7c-f43e203ec8b1" "577755d6-91c8-4815-bfca-dea5e362300c" "b47c89f9-f959-48f1-a955-b2642253350e" "fdde7243-3e9e-4044-98bb-b7ba8a6414b1" "f5a0f2a7-efa8-4fa4-9ba1-d9dc402364df" "7485cc2e-6a74-430a-9273-659334fbec21" "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd" "99544496-9a6e-46c4-940b-694918db5071" "3382e500-33fc-4803-9bea-56ab17e3d044" "6964c9bf-bdae-45fc-8978-c5eb8d22a810" "0f552276-6d15-43db-95e6-0572a50602fe" "22697b74-e156-4472-9aae-0bd2740cf9b9" "f365f293-4adc-4cdc-bcca-389425f3e2e6" "81c4abf6-9c3c-421e-8f11-bb78f3cfeb54" "14a0c9c7-3630-4ea3-957f-3807cd624636" "9e0c19af-f27f-4b04-99fd-689357ee1be8" "ff2eb6cd-7d1a-4eb9-b1f6-35e94db085d7" "01cee208-21ad-4c6c-a9d7-900f3f8b0214" "1f6fd79e-fb13-47ee-86a6-e152d4a9c36c" "bcc4ce43-e3c0-4040-b629-30c1a247038f" "e03ed372-2314-4555-9313-44f942884528" "2ab8d134-6781-441a-a3d1-4ad6262cd4e3" "3870839e-2424-494d-954f-244db4341234" "648480b7-1e76-40c2-a88e-4291543146fe" "2ce6a405-ebd7-4bb7-b682-3f7c146971e3" "5d640387-65eb-4d07-86ec-97f4ad8e3c8b" "c31b04d7-8009-4a63-935e-6185b226280e" "b9e5b157-21f7-4064-9356-42944722aa8e" "e0f74a51-d1b7-46f5-b0ad-d5b64de3d24b" "6d52b4da-fa8f-4b13-aa1d-26a4a4e661cd" "e1e51479-47c2-4acd-858a-910c66549292" "c400daec-62a8-4802-9455-02a4e93eba78" "7308596e-b6c9-4f11-a59a-bf38364ebd48" "2afef478-85f4-4e5c-baeb-b04f48e4a945" "b6d355fd-af3e-4b23-8949-f11cb93f0570" "454cf883-2480-4c24-a8e8-c078c522abef" "a34e4012-c32d-4186-ae83-4c791938f1f1" "5b34388f-4b84-4242-8acc-0514a75e57b5" "bccaa355-9245-4d13-87fb-705267683f41" "d6ed05b5-a1a9-4c6f-8832-ddfb02344b83" "24de8be6-1300-41be-9814-f698aacf1638" "ae2892c0-9e51-4db8-a6ef-790e8b75b506" "64e38f58-0fa1-4dee-8f41-fbac25a77f5f" "24a03356-60cd-4f92-9f79-4cc511dd6d7e" "d8396502-884f-4314-9e47-42545fe4be84" "20add41c-8575-4ebc-813a-50830c51f699" "428d5bc6-9c07-4852-ad14-aefeb49f380e" "c1604163-078b-4ef8-be0d-9740eb2636f1" "d7d3faa8-f7c1-4cf2-ba0b-23df648b3c7c" "ea3e293f-a982-484b-bc0c-c5d27afae2f7" "b9bf8d43-15d0-4460-869e-d44b06cd815a" "8f485322-2016-4642-a8b8-47f5c10ff599" "e1f9c39b-1a3a-4512-a123-30e93c38d37f" "a0a33606-3241-44bc-b91e-c09f10dd6054" "cdfe6ae3-fbad-4c98-9f2f-e91347856399" "5316b1b4-73e3-4431-b12c-2740e13d18f3" "5df38cbe-cd05-4140-ade8-dae74db385b5" "e7691a24-6d24-401f-9516-1fc7cf33f880" "c0cf3e5e-609f-474e-9fde-810285a0c31b" "f907e5df-dbe9-48a2-bc13-70884b1e56f0" "ee34edf4-05af-481d-a3c1-46371349f942" "60ab2373-1dbd-4884-8a9b-5ae626d245ce" "864a7140-73c9-4360-801e-cf056886b7fb" "60e4faa4-b39a-4539-b516-cdeceffd5541" "2c570b4c-6cbf-4424-a3e0-5bf60ae47d51" "2dee32c2-1b68-4eea-a80d-5c5668a24d45" "d11984bc-acb6-4801-8e99-12ee8c648dfe" "6175d1a2-0af7-43fb-8a53-212af7b72c9c" "e6ce2fbe-8f6b-442e-a9e3-cdb67a1c90a1" "83d5b8e1-945f-4917-9cd7-f9ddb6330b89" "d337321c-7d05-4449-b7ec-0ed0a611cd0d"])
(def kitchen-order ["20add41c-8575-4ebc-813a-50830c51f699" "24a03356-60cd-4f92-9f79-4cc511dd6d7e" "24de8be6-1300-41be-9814-f698aacf1638" "428d5bc6-9c07-4852-ad14-aefeb49f380e" "5df38cbe-cd05-4140-ade8-dae74db385b5" "864a7140-73c9-4360-801e-cf056886b7fb" "a0a33606-3241-44bc-b91e-c09f10dd6054" "a34e4012-c32d-4186-ae83-4c791938f1f1" "ae2892c0-9e51-4db8-a6ef-790e8b75b506" "bccaa355-9245-4d13-87fb-705267683f41" "c0cf3e5e-609f-474e-9fde-810285a0c31b" "c1604163-078b-4ef8-be0d-9740eb2636f1" "d6ed05b5-a1a9-4c6f-8832-ddfb02344b83" "d7d3faa8-f7c1-4cf2-ba0b-23df648b3c7c" "e1f9c39b-1a3a-4512-a123-30e93c38d37f" "e7691a24-6d24-401f-9516-1fc7cf33f880" "ee34edf4-05af-481d-a3c1-46371349f942" "f907e5df-dbe9-48a2-bc13-70884b1e56f0" "454cf883-2480-4c24-a8e8-c078c522abef" "5316b1b4-73e3-4431-b12c-2740e13d18f3" "60ab2373-1dbd-4884-8a9b-5ae626d245ce" "60e4faa4-b39a-4539-b516-cdeceffd5541" "64e38f58-0fa1-4dee-8f41-fbac25a77f5f" "8f485322-2016-4642-a8b8-47f5c10ff599" "b6d355fd-af3e-4b23-8949-f11cb93f0570" "b9bf8d43-15d0-4460-869e-d44b06cd815a" "cdfe6ae3-fbad-4c98-9f2f-e91347856399" "d8396502-884f-4314-9e47-42545fe4be84" "ea3e293f-a982-484b-bc0c-c5d27afae2f7" "5b34388f-4b84-4242-8acc-0514a75e57b5" "2402edca-bcdc-4ba0-a2a4-187568e9f7a1" "12e8f090-3da0-442c-9701-e34e19fd41e6" "6964c9bf-bdae-45fc-8978-c5eb8d22a810" "2207175d-3cc9-4495-a1ff-2df792aff5e5" "2afef478-85f4-4e5c-baeb-b04f48e4a945" "7308596e-b6c9-4f11-a59a-bf38364ebd48" "aea1b07a-be01-45fa-8a7b-b1ee7b2fca16" "fdde7243-3e9e-4044-98bb-b7ba8a6414b1" "e1e51479-47c2-4acd-858a-910c66549292" "577755d6-91c8-4815-bfca-dea5e362300c" "ff2eb6cd-7d1a-4eb9-b1f6-35e94db085d7" "b47c89f9-f959-48f1-a955-b2642253350e" "2a647d9b-4a02-4853-bad4-ca0f9201ed8b" "250b6a95-2dcc-4c23-b20c-c63cdec9c21f" "38db3fcc-b62b-4b0c-bd58-d40430117f05" "3c4bfe45-4c2a-4245-aa8f-2b8eb931e52e" "3dfc35ac-47a8-4985-80db-39ebdc383cb2" "3e99ba7a-be59-4f98-98a1-2ad17b01d14e" "45b8862f-6f2c-4a65-9245-552d69461235" "7e4b7ae1-d17b-4a51-bbca-3d5e45601fa8" "7f47b604-d812-49f5-86f2-bbe41e629fde" "8b77e903-6c87-4e48-b902-7035ca008374" "9f8d0bd9-4274-4f35-83fb-7916c43c77c3" "a5b567e7-d639-4f64-a915-d9c7c59ac264" "d4ac1410-3016-4e7a-8615-8c3b1a74c6ff" "da6b1d1d-08d1-4e5e-b086-031153d4033a" "f8e191a6-a0cc-4b75-82ec-b8a67e978f95" "ffed7113-6388-4473-ad82-60aaf0ac00f9" "cd3243c4-09a0-472d-970f-dc912ce74610" "6c0740a2-24a0-4aa7-9548-60c79bac6fec" "960f20f5-64e9-4c8a-ac8e-ce8e52a5e9e9" "e24ed0cd-c1a4-4e7c-92a1-84bcda090efa" "3599b722-c387-433f-a20a-5ef1bcc0fd34" "68da114e-5cd1-4c8b-b1e2-e24e325986a5" "99544496-9a6e-46c4-940b-694918db5071" "5381790d-3485-47ff-92bd-90d03f1f62a8" "cfccfc5d-4977-4eda-8716-0fc1e71944e8" "f5a0f2a7-efa8-4fa4-9ba1-d9dc402364df" "bb9d1c96-9301-4fc9-9563-73a607fce3f6" "f966190f-50fd-41aa-a7fa-f55bb3ae6d61" "36071b41-2b7b-4431-aad1-8a87b99327f6" "690fdb5c-711b-4b1b-918b-148d2a4eb355" "d33ad997-4d02-4437-8d35-db8e22fdb4b0" "01cee208-21ad-4c6c-a9d7-900f3f8b0214" "0f552276-6d15-43db-95e6-0572a50602fe" "49e653d8-08da-42c9-b19b-34320d400007" "7cc3f4e2-fc7a-41d5-a2c8-65e53d9ad641" "4373e821-8697-48ee-becf-9776f7a4e794" "14a0c9c7-3630-4ea3-957f-3807cd624636" "cec199c0-318a-475f-825c-4bbb7262778e" "22697b74-e156-4472-9aae-0bd2740cf9b9" "3382e500-33fc-4803-9bea-56ab17e3d044" "81c4abf6-9c3c-421e-8f11-bb78f3cfeb54" "f365f293-4adc-4cdc-bcca-389425f3e2e6" "9e0c19af-f27f-4b04-99fd-689357ee1be8" "0d12ab7e-e714-4365-b1b7-a600e009742b" "cfef02e2-13e8-40a1-8ea3-ac55b755e0e0" "41c7d982-8566-4bc0-8227-59125b18395a" "cfc2741b-c361-4d05-b71e-a2a118881400" "05da97fa-3bc1-40b5-a5bb-d1d341d96b44" "1ae57296-0493-4ac6-826e-549e4f4439a9" "2f989e52-c965-4f3c-af5b-e41ccd8b185f" "4caae747-2b97-4d1a-b477-629781beae3f" "8ce0dca4-db23-4ec8-a577-54e2caeeb802" "b287887d-346c-4644-ab84-4abc3eec81da" "c6f1817a-9536-4747-9160-fd263e53564f" "fa465e7b-c158-40b5-8d16-d2a156c476c6" "175c3da0-a4c2-4bc4-ab37-2cfef0012ca2" "152afeb9-c1ca-4256-8ec3-a90d4f6e5f5e" "3c541633-5f65-452f-aca5-313fcec1be31" "5eef57be-bc04-4f19-9d58-9c6eb2a5eddd" "66127547-25f2-4777-8c74-cad264c6a5e1" "8c98a110-b7cd-477c-ab8f-9a08074ab779" "c1bdcd61-b6ca-46fe-a42f-a411fcfa40bb" "c649995f-c56f-40d9-bb65-b6e57d9c1d73" "f053c6cd-0fc0-481a-8779-bac970a8dd8c" "5ef93777-a340-4f21-b06f-1fce3dba2703" "e3a78734-79bd-4304-bd7c-f43e203ec8b1" "3dc8331b-e1bf-4b59-9883-96bb855f9dfd" "1f6fd79e-fb13-47ee-86a6-e152d4a9c36c" "5281f0e4-cc6f-4a2b-91c2-a9c212919134" "83d5b8e1-945f-4917-9cd7-f9ddb6330b89" "b34db180-ff4f-45f6-9e06-4b20d58323e9" "b98dbaa1-6112-4f57-a879-5d58ed24bf91" "bcc4ce43-e3c0-4040-b629-30c1a247038f" "d71c24e8-85d4-4dc3-bb0d-1535a537fcf4" "e03ed372-2314-4555-9313-44f942884528" "e6ce2fbe-8f6b-442e-a9e3-cdb67a1c90a1" "3654b906-0dac-4db9-bf25-e4fbb9f4439f" "109a3ce3-78c7-4d8a-ab33-2226050a41d8" "1880eb8a-dafd-4ea2-a985-7789bc99e0a2" "1bdefb40-1eda-4f5a-9579-95fb75276bda" "282ef7bf-6d3a-4810-b331-5cf02d4e21b2" "2a74fe69-11c6-435c-8e57-2798243fabb2" "2b5e354e-6408-4b51-81a7-4cb7dfb6f39f" "2c78778a-2331-4d42-b7bb-02cc759f17da" "308d3d71-cbaf-437b-9f14-2d059515843e" "31bff67e-5285-475a-8d03-0228923d7215" "3bc4e5ec-8cff-4ff4-b080-50a2a3aed6ec" "43a6f610-b503-4116-9c0b-b540dd5a2778" "49f864fc-b05e-4136-90c2-28ad021a2c88" "4b7d675c-a303-4bda-9ead-6f216b86a814" "4d3c25e5-198b-40a2-9aca-467e64185384" "4e67d72f-44c4-464f-be33-05382c3c8080" "5138699f-9d01-400e-97d8-3a02bf90c5b9" "539ae848-13bd-410d-9830-32822b02443c" "6c2eae84-f52a-42b7-9448-199dbde69bb9" "7485cc2e-6a74-430a-9273-659334fbec21" "8997964c-df10-4e3c-8321-cc372a56ea94" "9edc6e43-a040-4829-88f8-de0eaa0a5209" "a4929cb9-8148-40c4-9875-c0860768b3b4" "afb01109-0750-4a59-9e63-7ad4fbb891b5" "cb5515eb-e63f-4a05-9c9f-1570220c8f71" "d337321c-7d05-4449-b7ec-0ed0a611cd0d" "e350c33d-a77a-45d5-b965-7e07b0f5588c" "fa805d56-ac9e-4975-8e1f-7615ad28b297" "fcf62e6b-597a-4711-ad79-1caf84243061" "030e4e1c-054d-4bb2-b208-c5cee01ca0e8" "132de4f5-99cf-4078-9063-ff5b89d54aec" "1d89bbb9-3052-45f7-9be9-d7a0c54c9b45" "589c086e-b06b-4451-aba2-90c46b8c1702" "5afade6b-f382-452c-91cb-edc761380983" "64e066ef-1e5b-4c67-aa34-acc6fee52ed8" "9580eac9-5902-4420-917d-7d6539c64c9b" "9597d110-d042-4203-990f-c339c2d61109" "95b94ddd-c86f-4502-b809-a9b050d28e5b" "bf5870c3-64f9-42c9-a584-8371dbb5358e" "d06107aa-68b5-41f0-8256-f124e5d0f240" "e67009b0-be0f-4c3e-b23b-75ff752fa643" "539de6ed-3f2c-40b2-a05f-904a388844a8" "54c81443-15b7-48b9-883f-0d88cc131e5e" "94f58d5a-221b-48d4-9f9e-118d1fdce128" "15e8d610-ce9f-405b-957c-5f4fb6249b20" "6a5fb199-bac0-4db1-bc2b-16d65c0ccfdb" "a1ab22e1-8f4b-411e-a9e9-a1c5efa6c51d" "c181aef6-0e09-43c1-85d1-2aaabdb1ce6a" "f29557a1-d028-4efa-860d-562ad6fe8c56" "73233dd2-991a-40ea-8e3b-384bcf2ac408" "a63da408-9142-43aa-9ffa-1eb6b095c0bd" "be78e544-68c8-4a06-89ba-6def6d88152d" "c400daec-62a8-4802-9455-02a4e93eba78" "ed57f4f9-d03b-49ce-8350-d9140918de2b" "ef71e8f8-d318-41da-9421-41f1b39c8e1a" "00c1ba7e-3c7a-4ac5-8842-6d2f57b088c8" "2ab8d134-6781-441a-a3d1-4ad6262cd4e3" "2ce6a405-ebd7-4bb7-b682-3f7c146971e3" "3870839e-2424-494d-954f-244db4341234" "648480b7-1e76-40c2-a88e-4291543146fe" "874cf542-721a-4a4f-b690-78e0048d354e" "2c570b4c-6cbf-4424-a3e0-5bf60ae47d51" "2dee32c2-1b68-4eea-a80d-5c5668a24d45" "6175d1a2-0af7-43fb-8a53-212af7b72c9c" "d11984bc-acb6-4801-8e99-12ee8c648dfe" "5d640387-65eb-4d07-86ec-97f4ad8e3c8b" "c31b04d7-8009-4a63-935e-6185b226280e" "6d52b4da-fa8f-4b13-aa1d-26a4a4e661cd" "b9e5b157-21f7-4064-9356-42944722aa8e" "e0f74a51-d1b7-46f5-b0ad-d5b64de3d24b"])
(defn sort-order-indexes [sort-order]
  (into {} (map-indexed (fn [idx id] [id idx]) sort-order)))

(defn sory-by-order [sort-order ingredients]
  (let [sort-order-map (sort-order-indexes sort-order)]
    (sort-by (fn [{:keys [ingredient/id]}] (sort-order-map id)) ingredients)))

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
                                     (< (.indexOf kitchen-category-order category1)
                                        (.indexOf kitchen-category-order category2))))
                          (sory-by-order kitchen-order))))]
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
