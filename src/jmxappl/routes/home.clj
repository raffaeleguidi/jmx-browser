(ns jmxappl.routes.home
  (:require [compojure.core :refer :all]
            [jmxappl.layout :as layout]
            [jmxappl.util :as util]
            [clojure.java.jmx :as jmx])
  (:use [taoensso.timbre :only [trace debug info warn error fatal]]))

(defn home-page []
  (layout/render
    "home.html" {:content (util/md->html "/md/docs.md")}))

(defn about-page []
	(layout/render "about.html"))

(let [jmx-class-name "java.lang:type=Threading"]

    (defn pool-page [host port prefix]
      (if 
        (or (clojure.string/blank? host) (clojure.string/blank? port))
           (layout/render "pool.html" {:host ""
	                                      :port 0
	                                      :prefix prefix
	                                      :count 0
	                                      :thread-names nil})

    (jmx/with-connection {:host host :port port}
	    (defn threadInfo [id]
	         ;(info "threadInfo" id)
		     (jmx/invoke jmx-class-name :getThreadInfo id))
			
	      (defn myThread [id]
	         ;(info "myThread" id)
	         (try 
	           (let [this-thread (threadInfo id)]
		           [id (.get this-thread "threadName") (.get this-thread "threadState")  (.get this-thread "lockName")]) 
	           (catch Exception e (str "caught exception: " (.getMessage e)))))
	
		    (defn allThreadIds []
		      (jmx/read jmx-class-name :AllThreadIds))
		
	      (let [allThreads (with-local-vars [threads []]                
		                        (doseq [id (allThreadIds)]
		                          (let [tt (myThread id)]
	                              (var-set threads (if (.startsWith (str (nth tt 1)) (str prefix)) (conj @threads tt) (set @threads)))))
	                         @threads)]
	
	        (layout/render "pool.html" {:host host
	                                    :port port
	                                    :prefix prefix
	                                    :count (if (empty? allThreads) 0 (.size allThreads))
	                                    :thread-names allThreads}))))
        
        )
      )

   
 (defroutes home-routes
  (GET "/" [] (pool-page "" 0 ""))
  (GET "/about" [] (about-page))
  ;(GET "/pool/:host/:port" [host port] (pool-page host port ""))
  (GET "/pool" [host port prefix] (pool-page host port (str prefix))))