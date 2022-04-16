(ns dvliman.demo-test
  (:require [dvliman.demo :as demo]
            [clojure.test :refer :all]
            [ring.mock.request :as mock]))


(-> (mock/request :post "/login")
    demo/app)

#_(deftest run-through
  (-> (mock/request :post "/login")
    demo/app))
