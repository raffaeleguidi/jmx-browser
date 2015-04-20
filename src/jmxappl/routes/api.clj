(ns jmxappl.routes.api
  (:require [compojure.core :refer :all]
            [jmxappl.layout :as layout]
            [jmxappl.util :as util]
            [clojure.pprint :as p]
            [clojure.java.jmx :as jmx])
  (:use [taoensso.timbre :only [trace debug info warn error fatal]]))

(use '[clojure.string :only (join split)])

(def jmx-threading "java.lang:type=Threading")
(def jmx-gc-base "java.lang:type=GarbageCollector")

(defn gc [algorithm]
   (jmx/mbean (str jmx-gc-base ",name=" algorithm)))

(defn gc-last [algorithm]
   (get (gc algorithm) :LastGcInfo))

(defn gc-invoke-collection []
  (jmx/invoke "java.lang:type=Memory" :gc))

(defn threadInfo [id]
   (jmx/invoke jmx-threading :getThreadInfo id))

(defn myThread [id]
   (try
     (let [this-thread (threadInfo id)]
       {:id id
        :threadName (.get this-thread "threadName")
        :threadState (.get this-thread "threadState")
        :lockName (.get this-thread "lockName")})
     (catch Exception e
       {:id id
        :threadState (.getMessage e)
        :threadName ""
        :lockName ""})))

(defn allThreadIds []
  (jmx/read jmx-threading :AllThreadIds))

(defn filtered-threads [prefix]
  (with-local-vars [threads []]
         (doseq [id (allThreadIds)]
           (let [tt (myThread id)]
             (var-set threads
                (if (.startsWith (str (get tt :threadName)) (str prefix))
                  (conj @threads tt) (set @threads)))))
        @threads))

(defn pool-api [host port prefix]
  (let [startedAt (System/currentTimeMillis)]
    (jmx/with-connection {:host host :port port}

      (let [allThreads (filtered-threads prefix)]
         (let [parked (for [t allThreads :when (.startsWith (str (get t :lockName )) "java.util.concurrent.CountDownLatch")] t)]
            (let [acceptors (for [t allThreads :when (if (not(= -1 (int (.indexOf (str (get t :threadName)) "Acceptor")))) true false)] t)]
                {:body {:host host
                         :port port
                         :prefix prefix
                         :parked (util/sizeOrZero parked)
                         :acceptors (util/sizeOrZero acceptors)
                         :workers (- (util/sizeOrZero allThreads) (util/sizeOrZero acceptors))
                         :count (util/sizeOrZero allThreads)
                         :timeTaken (- (System/currentTimeMillis) startedAt)
                         ;:threads allThreads
                         }}))))))

(defn oracle-last-gc [last]
  {:id (:id last)
   :style "oracle-1.7"
   :duration (:duration last)
   :startTime (:startTime last)
   :endTime (:endTime last)})

(defn ibm-last-gc [last]
  {:id (:CollectionCount last)
   :style "ibm-1.6"
   :duration (- (:LastCollectionEndTime last) (:LastCollectionStartTime last))
   :startTime (:LastCollectionStartTime last)
   :endTime (:LastCollectionEndTime last)})

(defn last-info [gc]
  (if (:LastCollectionStartTime  gc) (ibm-last-gc gc) (oracle-last-gc (:LastGcInfo  gc)) ))


(defn gc-api [host port algorithm force-gc]
  "all things garbage collection" ;http://www.fasterj.com/articles/oraclecollectors1.shtml
  (jmx/with-connection {:host host :port port}
    (if (empty? algorithm)
       (do {:body {:algorithms (for [s (jmx/mbean-names (str jmx-gc-base ",name=*"))](str s))}})
       (do
         (info (jmx/mbean-names (str jmx-gc-base ",name=*")))

         (if (or (= "true" force-gc) (= "yes" force-gc))
            (do
              (info "forcing gc")
              (gc-invoke-collection)
              (info "done forcing gc")))

         {:body {:host host
                  :port port
                  :GC {
                  :algorithm algorithm
                  :count (get (gc algorithm) :CollectionCount)
                  :time (get (gc algorithm) :CollectionTime)
                  ; handle divide by zero exception before re-enabling
                  ;:avg (/ (get (gc algorithm) :CollectionTime) (get (gc algorithm) :CollectionCount))
                  :last (last-info (gc algorithm))}}}))))

(defroutes api-routes
  (GET "/API/gc" [host port algorithm force-gc] (gc-api host port algorithm force-gc))
  (GET "/API/pool" [host port prefix] (pool-api host port prefix)))
