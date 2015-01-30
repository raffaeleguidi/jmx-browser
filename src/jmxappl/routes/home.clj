(ns jmxappl.routes.home
  (:require [compojure.core :refer :all]
            [jmxappl.layout :as layout]
            [jmxappl.util :as util]
            [clojure.java.jmx :as jmx])
  (:use [taoensso.timbre :only [trace debug info warn error fatal]]))

(defn home-page []
  (layout/render
    "home.html" {:content (util/md->html "/md/docs.md")}))

(let [jmx-class-name "java.lang:type=Threading"]

    (defn pool-page [host port name]
          (info "before jmx/with-connection")
            (defn allThreadIds []
              (jmx/read jmx-class-name :AllThreadIds))
          (info "local thread ids:" (for [id (allThreadIds)] id))
          (jmx/with-connection {:host host :port port}

           (defn threadInfo [id]
                ;(info "threadInfo" id)
	            (jmx/invoke jmx-class-name :getThreadInfo id))
		
             (defn myThread [id]
                ;(info "myThread" id)
                (try 
                  (let [this-thread (threadInfo id)]
	                  [id (.get this-thread "threadName") (.get this-thread "threadState")]) 
                  (catch Exception e (str "caught exception: " (.getMessage e)))))

	           (defn allThreadIds []
	             (jmx/read jmx-class-name :AllThreadIds))
	
             (info "in jmx/with-connection")
   
             (info "remote thread ids:" (for [id (allThreadIds)] id))
              
             (let [allThreads (with-local-vars [threads []]                
	                               (doseq [id (allThreadIds)]
	                                 (let [tt (myThread id)]
	                                   ;(info "tt=" tt)
	                                   (var-set threads (conj @threads tt))))
						    @threads)]
                ;(info "allThreads" allThreads)
                (layout/render "pool.html" {:prefix name
                                            :count (.length allThreads)
                                            :thread-names allThreads})))))
   
(defn about-page []
	(layout/render "about.html"))

 (defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (GET "/pool/:host/:port/:name" [host port name] (pool-page host port name)))