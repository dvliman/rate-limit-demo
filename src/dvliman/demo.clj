(ns dvliman.demo
  (:require [reitit.ring :as ring]
            [ring.util.response :as response]))

(defn update-queue [queue new-bucket new-timestamp capacity interval-ms]
  (let [pruned-queue
        (loop [q queue]
          (if-let [{:keys [timestamp]} (first q)]
            (if (<= timestamp (- new-timestamp interval-ms))
              (do (prn "deleting")
                (recur (pop q)))
              q)
            q))]
    (if (>= (count pruned-queue) capacity)
      (do (prn "hitting capacity" (count pruned-queue))
          pruned-queue)
      (conj pruned-queue {:bucket new-bucket
                          :timestamp new-timestamp}))))

(def ^:dynamic *queue* (atom (clojure.lang.PersistentQueue/EMPTY)))

(defn create-rate-limit-middleware [{:keys [capacity interval-ms bucket-fn]}]
  (fn [handler]
    (fn [request]
      (let [;;bucket (bucket-fn request)
            bucket (str (System/currentTimeMillis))
            now (System/currentTimeMillis)
            _ (prn (seq @*queue*))
            new-queue (swap! *queue*
                             #(update-queue % bucket now capacity interval-ms))
            _ (prn "last" (last new-queue))]
        (if (not= bucket (:bucket (last new-queue)))
          {:status 429 :body {:error :exceeded-rate-limit}}
          (handler request))))))

(defn routes []
  [["/login"
    {:post
     {:handler
      (constantly
       (response/response {:status :logged-in}))}}]
   ["/health"
    {:get
     {:handler
      (constantly
       (response/response {:status :ok}))}}]])

(def rate-limit-option
  {:capacity 1
   :interval-ms (* 1 1000)
   :bucket-fn #(:remote-addr %)}) ;; by IP address

(def app
  (ring/ring-handler
   (ring/router
    (routes)
    {:data
     {:middleware
      [(create-rate-limit-middleware rate-limit-option)]}})))
