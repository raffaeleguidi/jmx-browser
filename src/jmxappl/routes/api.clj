(ns jmxappl.routes.api
  (:require [compojure.core :refer :all]
            [jmxappl.layout :as layout]
            [jmxappl.util :as util]
            [clojure.java.jmx :as jmx])
  (:use [taoensso.timbre :only [trace debug info warn error fatal]]))

  
(defn sizeOrZero [set] 
  (if (empty? set) 0 (.size set)))
         
(defn pool-api [host port prefix]
  (let [jmx-class-name "java.lang:type=Threading"]
    (jmx/with-connection {:host host :port port}
			   (defn threadInfo [id]
			      (jmx/invoke jmx-class-name :getThreadInfo id))
				
			     (defn myThread [id]
			        ;(info "myThread" id)
			        (try 
			          (let [this-thread (threadInfo id)]
			            [id (.get this-thread "threadName") (.get this-thread "threadState")  (.get this-thread "lockName")]) 
			          (catch Exception e 
			            [id (.getMessage e) "" ""])))
			
			     (defn allThreadIds []
			       (jmx/read jmx-class-name :AllThreadIds))))					
	 (let [allThreads (with-local-vars [threads []]                
		                   (doseq [id (allThreadIds)]
		                     (let [tt (myThread id)]
	                         (var-set threads (if (.startsWith (str (nth tt 1)) (str prefix)) (conj @threads tt) (set @threads)))))
	                    @threads)]     
      (let [parked (for [t allThreads :when (.startsWith (str (nth t 3)) "java.util.concurrent.CountDownLatch")] t)]
         (let [acceptors (for [t allThreads :when (if (not(= -1 (int (.indexOf (str (second t)) "Acceptor")))) true false)] t)]
             {:body {:host host
                      :port port
                      :prefix prefix
                      :parked (sizeOrZero parked)
                      :acceptors (sizeOrZero acceptors)
                      :workers (- (sizeOrZero allThreads) (sizeOrZero acceptors))
                      :count (sizeOrZero allThreads)
                      :threads (sort-by last (sort-by second allThreads))}}))))
   
(defroutes api-routes
  (GET "/API/pool" [host port prefix] (pool-api host port (str prefix))))