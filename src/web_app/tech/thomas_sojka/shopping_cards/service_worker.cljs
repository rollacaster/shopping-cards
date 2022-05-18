(ns tech.thomas-sojka.shopping-cards.service-worker)

(defn add-resources-to-cache [resources]
  (-> (.open js/caches "v1")
      (.then (fn [cache]
               (.addAll cache (into-array resources))))
      (.catch (fn [e] (js/console.error e)))))

(.addEventListener js/self "install"
                   (fn [^js event]
                     (.waitUntil event
                                 (add-resources-to-cache ["/"
                                                          "/index.html"
                                                          "/main.js"
                                                          "/shared.js"
                                                          "https://unpkg.com/tachyons@4.12.0/css/tachyons.min.css"
                                                          "/css/styles.css"]))))

(defn put-in-cache [request, response]
  (-> (.open js/caches "v1")
      (.then (fn [cache] (.put cache request response)))
      (.catch (fn [e] (js/console.error e)))))

(defn cache-first [request]
  (-> (.match js/caches request)
      (.then (fn [response-from-cache]
               (if response-from-cache
                 response-from-cache
                 (-> (js/fetch request)
                     (.then (fn [reponse-from-network]
                              (put-in-cache request (.clone reponse-from-network))
                              reponse-from-network))))))
      (.catch (fn [e] (js/console.error e)))))

(.addEventListener js/self "fetch"
                   (fn [^js event] (.respondWith event (cache-first (.-request event)))))
