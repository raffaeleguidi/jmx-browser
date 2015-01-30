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

    (defn pool-page [host port name]
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
	
         (let [allThreads (with-local-vars [threads []]                
	                           (doseq [id (allThreadIds)]
	                             (let [tt (myThread id)]
	                               (var-set threads (conj @threads tt))))                                
                            @threads)]

           (let [filtered (filter (fn [tt]
                                    (.startsWith (nth tt 1) name))
                                  allThreads)]

            (layout/render "pool.html" {:prefix name
                                        :count (.size filtered)
                                        :thread-names filtered}))))))
   
 (defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (GET "/pool/:host/:port" [host port] (pool-page host port ""))
  (GET "/pool/:host/:port/:name" [host port name] (pool-page host port (str name))))