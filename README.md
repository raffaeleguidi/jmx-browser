# jmxappl

FIXME

## What is it

The application exposes restful services to inspect GC and threads of IBM and Oracle JVMs over RMI (non protected)

    http://<host>:<port>/
    
    A sample web page to inspect threads
    

    http://<host>:<port>/API/gc?host=<jmxserver>&port=<jmxport>&algorithms
    
    List GC algorithms of the target JVM
    
    
    http://<host>:<port>/API/gc?host=<jmxserver>&port=<jmxport>&algorithm=<algorithm>
    
    List GCs of the target JVM for the specified algorithm


    http://<host>:<port>/API/pool?host=<jmxserver>&port=<jmxport>&prefix=<prefix>
    
    Lists threads for the specified prefix. Acceptor and blocked threads are returned    
    

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
