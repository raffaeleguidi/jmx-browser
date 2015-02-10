(ns jmxappl.routes.api
  (:require [compojure.core :refer :all]
            [jmxappl.layout :as layout]
            [jmxappl.util :as util]
            [clojure.java.jmx :as jmx])
  (:use [taoensso.timbre :only [trace debug info warn error fatal]]))

(use '[clojure.string :only (join split)])

(def jmx-threading "java.lang:type=Threading")
(def jmx-gc-base "java.lang:type=GarbageCollector")
(def jmx-gc "")
;(def jmx-gc "java.lang:type=GarbageCollector,name=PS MarkSweep")
;(def jmx-gc "java.lang:name=MarkSweepCompact,type=GarbageCollector")
;GC_NAME = "java.lang:name=MarkSweepCompact,type=GarbageCollector";

(defn gc []
   (jmx/mbean jmx-gc))
  
(defn gc [algorithm]
   (jmx/mbean (str jmx-gc-base ",name=" algorithm)))
  
(defn gc-last []
   (get (gc) :LastGcInfo))
 
(defn gc-last [algorithm]
   (get (gc algorithm) :LastGcInfo))
 
(defn gc-count []
   (get (gc) :CollectionCount))
 
(defn gc-time []
   (get (gc) :CollectionTime))
 
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
                       ;:threads allThreads
                       }})))))

(defn pool2-api [host port prefix]
  (jmx/with-connection {:host host :port port}
   
    (let [allThreads (filtered-threads prefix)]     
       (let [parked (for [t allThreads :when (.startsWith (str (get t :lockName )) "java.util.concurrent.CountDownLatch")] t)]
          (let [acceptors (for [t allThreads :when (if (not(= -1 (int (.indexOf (str (get t :threadName)) "Acceptor")))) true false)] t)]
              {:body {:host host
                       :port port
                       :pool {
                         :prefix prefix
                         :parked (util/sizeOrZero parked)
                         :acceptors (util/sizeOrZero acceptors)
                         :workers (- (util/sizeOrZero allThreads) (util/sizeOrZero acceptors))
                         :count (util/sizeOrZero allThreads)
                         :threads allThreads
                       }}})))))
   
(defn gc-api [host port algorithm force-gc]
  ;see http://www.fasterj.com/articles/oraclecollectors1.shtml
  (jmx/with-connection {:host host :port port}
    (if (empty? algorithm)
       (do 
         (info (jmx/mbean-names (str jmx-gc-base ",name=*")))
         {:body {:algorithms (for [s (jmx/mbean-names (str jmx-gc-base ",name=*"))](str s))}}
         )
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
                  :last {
                    :id (get (gc-last algorithm) :id)
                    :duration (get (gc-last algorithm) :duration)
                    :startTime (get (gc-last algorithm) :startTime)
                    :endTime (get (gc-last algorithm) :endTime) 
                  }}}}))))
   
(defroutes api-routes
  (GET "/API/gc" [host port algorithm force-gc] (gc-api host port algorithm force-gc))
  (GET "/API/poolWithThreads" [host port prefix] (pool2-api host port prefix))
  (GET "/API/pool" [host port prefix] (pool-api host port prefix)))






