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
                                 ;(info "tt.1" (nth tt 1) (.startsWith (nth tt 1) prefix))
;                                 (var-set threads (if (.startsWith (str (nth tt 1)) prefix) (conj @threads tt) threads))))
                                 (var-set threads (if (.startsWith (str (nth tt 1)) (str prefix)) (conj @threads tt) (set @threads)))))
;	                               (var-set threads (conj @threads tt))))                                
                            @threads)]

           (layout/render "pool.html" {:host host
                                       :port port
                                       :prefix prefix
                                       :count (.size allThreads)
                                       :thread-names allThreads})))))

   
 (defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (GET "/pool/:host/:port" [host port] (pool-page host port ""))
  (GET "/pool" [host port prefix] (pool-page host port (str prefix))))