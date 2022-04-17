(ns dvliman.demo-test
  (:require [dvliman.demo :as demo]
            [reitit.ring :as ring]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))

(defn make-test-router [options]
  (ring/ring-handler
   (ring/router
    (demo/routes)
    {:data
     {:middleware [(demo/wrap-rate-limit options)]}})))

(def default-options
  {:capacity 1
   :interval-ms (* 1 1000)
   :bucket-fn #(:remote-addr %)})

(deftest capacity-test
  (testing "2 requests (same IP) with capacity of 1, 1 should be rejected"
    (let [router (make-test-router default-options)]
      (is (= 200 (:status (-> (mock/request :post "/login") router))))
      (Thread/sleep 1)
      (is (= 429 (:status (-> (mock/request :post "/login") router)))))))

(deftest reset-after-interval-window
  (testing "after interval window, rate limit should be open again"
    (let [router (make-test-router default-options)]
      (is (= 200 (:status (-> (mock/request :post "/login") router))))
      (Thread/sleep (inc (:interval-ms default-options)))
      (is (= 200 (:status (-> (mock/request :post "/login") router)))))))

(deftest capacity-per-ip-address
  (testing "2 requests (different IP) with capacity of 1, should be ok"
    (let [router (make-test-router default-options)
          req1 (-> (mock/request :post "/login") (assoc :remote-addr "47.151.244.186"))
          req2 (-> (mock/request :post "/login") (assoc :remote-addr "47.151.244.187"))]
      (is (= 200 (:status (-> req1 router))))
      (is (= 200 (:status (-> req2 router)))))))

(deftest middleware-for-all-urls
  (testing "rate limit middleware applies to all urls"
    (let [router (make-test-router default-options)]
      (is (= 200 (:status (-> (mock/request :post "/login") router))))
      (Thread/sleep 1)
      (is (= 429 (:status (-> (mock/request :get "/health") router)))))))
