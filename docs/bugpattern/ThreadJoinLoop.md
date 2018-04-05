Thread.join() can be interrupted, and so requires users to catch
InterruptedException. Most users should be looping until the join() actually
succeeds.
