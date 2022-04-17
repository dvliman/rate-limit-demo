(ns dvliman.demo
  (:require [reitit.ring :as ring]
            [ring.util.response :as response]))

(defn update-state [state bucket timestamp capacity interval-ms]
  (if-let [requests-queue (get state bucket)]
    (let [pruned-queue (loop [q requests-queue]
                         (if-let [old-timestamp (first q)]
                           (if (<= old-timestamp (- timestamp interval-ms))
                             (recur (pop q))
                             q)
                           q))]
      (if (>= (count pruned-queue) capacity)
        (assoc state bucket pruned-queue)
        (assoc state bucket (conj pruned-queue timestamp))))
    (assoc state bucket (conj (clojure.lang.PersistentQueue/EMPTY) timestamp))))

(defn create-rate-limit-middleware [{:keys [capacity interval-ms bucket-fn]}]
  (let [state (atom {})]
    (fn [handler]
      (fn [request]
        (let [bucket (bucket-fn request)
              now (System/currentTimeMillis)
              new-state (swap! state
                               #(update-state % bucket now capacity interval-ms))]
          (if (not= now (last (get new-state bucket)))
            {:status 429 :body {:error :exceeded-rate-limit}}
            (handler request)))))))

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
