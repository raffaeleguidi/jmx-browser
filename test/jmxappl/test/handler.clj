(ns jmxappl.test.handler
  (:use clojure.test
        ring.mock.request
        jmxappl.handler)
  (:require [clojure.java.jmx :as jmx]))


(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= 200 (:status response)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= 404 (:status response)))))

  (testing "put new test here"
      (is true))

  ;(testing "test jmx local connection"
  ; (jmx/with-connection (jmx/obtain-local-connection "10379")
  ;        (jmx/mbean "java.lang:type=Memory")))
  )
