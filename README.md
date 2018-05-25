# iq-test

just my test assignment


command line options:

--port	HTTP port to serve requests on, default is 8080

--backup-dir	Directory to store backups, default is /tmp/userIds

--clean	Removes all existing backups in specified backup directory

--redis	Uses redis-server for persistance. Needs a port

--help	Print help message and exit


the approach is to use immutable scala set and add to it inside syncronized block

to persist the state between restarts - set is serialized in textual file every 30 seconds (and in a shutdown hook)


some notes:

- for json parsing "akka-http-spray-json" was used, i don't think that it violates requirement to use only standard library, as akka-http is mandatory

- sbt-assemly plugin is used 

- logging is naive, 'cause the assignment clearly states to use only standard library

- command line argument parsing is naive for the same reason

- backups are never removed, unless server is run with "--clean" argument

- redis is not favorable option, 'cause its perfomance is worse than naive "syncronized immutable set" approach (maybe 'cause i'm doing it wrong)
