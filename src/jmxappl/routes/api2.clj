(ns jmxappl.routes.api2
  (:require [compojure.core :refer :all]
            [jmxappl.layout :as layout]
            [jmxappl.util :as util]
            [clojure.java.jmx :as jmx])
  (:use [taoensso.timbre :only [trace debug info warn error fatal]]))

(def jmx-threading "java.lang:type=Threading")
(def jmx-gc "java.lang:type=GarbageCollector,name=PS MarkSweep")

(defn sizeOrZero [set] 
   (if (empty? set) 0 (.size set)))

(defn gc-endtime []
   (get (get (jmx/mbean jmx-gc) :LastGcInfo) :endTime)) 
 
(defn gc-duration []
   (get (get (jmx/mbean jmx-gc) :LastGcInfo) :duration))
 
(defn gc-count []
   (get (jmx/mbean jmx-gc) :CollectionCount))
 
(defn gc-time []
   (get (jmx/mbean jmx-gc) :CollectionTime))
 
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

         
(defn pool-api [host port prefix force-gc]
  (jmx/with-connection {:host host :port port}
   
    (comment (jmx/invoke "java.lang:type=Memory" :gc)
    (info "gc-all" (jmx/mbean jmx-gc))			
    (info "gc-count" (get (jmx/mbean jmx-gc) :CollectionCount))			
    (info "gc-time" (get (jmx/mbean jmx-gc) :CollectionTime))			
    (info "gc-time" (get (jmx/mbean jmx-gc) :StartTime)))
  
    (comment (if (or (= "true" force-gc) (= "yes" force-gc))
               (do 
                 (info "forcing gc")
                 (gc-invoke-collection)
                 (info "done forcing gc"))))
  
    (let [allThreads 
          (with-local-vars [threads []]                
	           (doseq [id (allThreadIds)]
	             (let [tt (myThread id)]
                 (var-set threads 
                    (if (.startsWith (str (get tt :threadName)) (str prefix)) 
                      (conj @threads tt) (set @threads)))))
            @threads)]     
       (let [parked (for [t allThreads :when (.startsWith (str (get t :lockName )) "java.util.concurrent.CountDownLatch")] t)]
          (let [acceptors (for [t allThreads :when (if (not(= -1 (int (.indexOf (str (get t :threadName)) "Acceptor")))) true false)] t)]
              {:body {:host host
                       :port port
                       :threads {
                         :prefix prefix
                         :parked (sizeOrZero parked)
                         :acceptors (sizeOrZero acceptors)
                         :workers (- (sizeOrZero allThreads) (sizeOrZero acceptors))
                         :count (sizeOrZero allThreads)
                       ;:threads allThreads
                       :GC {
                       :count (gc-count)
                       :time (gc-time)
                       :last {
                         :duration (gc-duration)
                         :endTime (gc-endtime) 
                       }}}}})))))
   
(defroutes api2-routes
  (GET "/API2/pool" [host port prefix force-gc] (pool-api host port (str prefix) force-gc)))