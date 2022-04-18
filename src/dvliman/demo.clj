(ns dvliman.demo
  (:require [reitit.ring :as ring]
            [ring.util.response :as response])
  (:import [clojure.lang PersistentQueue]
           [java.util UUID]))

(defn prune-queue [requests-queue cutoff]
  (loop [q requests-queue]
    (if-let [old-timestamp (:ts (first q))]
      (if (<= old-timestamp cutoff)
        (recur (pop q))
        q)
      q)))

(defn update-state [state bucket entry
                    {:keys [capacity interval-ms]}]
  (if-let [requests-queue (get state bucket)]
    (let [pruned-queue (prune-queue requests-queue (- (:ts entry) interval-ms))]
      (if (>= (count pruned-queue) capacity)
        (assoc state bucket pruned-queue)
        (assoc state bucket (conj pruned-queue entry))))
    (assoc state bucket (conj (PersistentQueue/EMPTY) entry))))

(defn wrap-rate-limit [{:keys [bucket-fn] :as opts}]
  (let [state (atom {})]
    (fn [handler]
      (fn [request]
        (let [bucket (bucket-fn request)
              entry {:ts (System/currentTimeMillis)
                     :id (UUID/randomUUID)}]
          (swap! state #(update-state % bucket entry opts))
          (if (not= (:id entry) (:id (last (get @state bucket))))
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
   :interval-ms 1000
   :bucket-fn :remote-addr}) ;; by IP address

(def app
  (ring/ring-handler
   (ring/router
    (routes)
    {:data
     {:middleware
      [(wrap-rate-limit rate-limit-option)]}})))
