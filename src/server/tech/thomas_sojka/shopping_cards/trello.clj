(ns tech.thomas-sojka.shopping-cards.trello
  (:require [clj-http.client :as client]
            [tech.thomas-sojka.shopping-cards.auth :refer [creds-file]]
            [tick.core :refer [now]]))

(def trello-api "https://api.trello.com/1")

(def klaka-board-id "48aas65T")

(defn load-trello-lists [board-id]
  (client/get (str trello-api "/boards/" board-id "/lists")
              {:query-params
               {:key (:trello-key creds-file)
                :token (:trello-token creds-file)}
               :as :json
               :throw-entire-message? true}))

(defn create-trello-shopping-card [list-id]
  (:body (client/post (str trello-api "/cards/")
                      {:query-params
                       {:key (:trello-key creds-file)
                        :token (:trello-token creds-file)
                        :name (str "Einkaufen " (apply str (take 10 (str (now)))))
                        :idList list-id}
                       :as :json
                       :throw-entire-message? true})))

(defn create-trello-checklist [card-id]
  (:body
   (client/post (str trello-api "/checklists")
                {:query-params
                 {:key (:trello-key creds-file)
                  :token (:trello-token creds-file)
                  :idCard card-id}
                 :as :json
                 :throw-entire-message? true})))

(defn create-trell-checklist-item [checklist-id item]
  (:body (client/post (str trello-api (str "/checklists/" checklist-id "/checkItems"))
                      {:query-params
                       {:key (:trello-key creds-file)
                        :token (:trello-token creds-file)
                        :name item}
                       :as :json
                       :throw-entire-message? true})))

(defn create-klaka-shopping-card [ingredients]
  (let [list-id (:id (first (:body (load-trello-lists klaka-board-id))))
        card-id (:id (create-trello-shopping-card list-id))
        checklist-id (:id (create-trello-checklist card-id))]
    (doseq [ingredient ingredients]
      (create-trell-checklist-item checklist-id ingredient))
    card-id))

