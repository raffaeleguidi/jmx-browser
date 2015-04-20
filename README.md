# jmx-browser

This application exposes restful services to inspect GC and threads of IBM and Oracle JVMs over RMI (non protected)

## Usage

Main page - useful to check connections and interactively inspect threads
    
    http://<host>:<port>/
    
API call to list GC algorithms of the target JVM (returns a JSON payload)
    
    http://<host>:<port>/API/gc?host=<jmxserver>&port=<jmxport>&algorithms
    
List GCs of the target JVM for the specified algorithm (returns a JSON payload)

    http://<host>:<port>/API/gc?host=<jmxserver>&port=<jmxport>&algorithm=<algorithm>
        
Lists threads for the specified prefix. Acceptor and blocked threads are returned (returns a JSON payload)

    http://<host>:<port>/API/pool?host=<jmxserver>&port=<jmxport>&prefix=<prefix>
    

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start the application in development run:

    lein ring server
    
To build and pack the application in a single jar run:

    lein ring uberjar
    
To run the packed application run:

    java -jar jmx-appl.jar <port>
    
Default port is 4000 - set $PORT env variable if you wish

## License

Copyright © 2015 Raffaele P. Guidi
