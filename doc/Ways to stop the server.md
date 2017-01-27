Possible ways to stop the server

1. Error raised in a service thread
   1. No need to stop the server
   2. Construct a RMIException.
   3. Report the error to the client, if possible.
   4. Call service_error with RMIException
2. Error raised in listen thread
   1. Call listen_error with the exception
   2. If listen_error returns true, ignore the exception
   3. Otherwise, stop the server and trigger stopped with the exception
3. Error raised in main thread
   1. Rethrow the RMIException to the caller.
4. stop() called
   1. Try to stop the listen_thread
   2. On listen_thread stopping, trigger stopped with null
