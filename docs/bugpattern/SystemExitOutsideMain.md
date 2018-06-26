Calling System.exit terminates the java process and returns a status code. Since
it is disruptive to shut down the process within library code, System.exit
should not be called outside of a main method.

Instead of calling System.exit consider throwing an unchecked exception to
signal failure.
