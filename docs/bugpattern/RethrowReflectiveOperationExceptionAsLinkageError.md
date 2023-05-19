Consider using `LinkageError` instead of `AssertionError` when rethrowing
reflective exceptions as unchecked exceptions, since it conveys more information
when reflection fails due to an incompatible change in the classpath.
