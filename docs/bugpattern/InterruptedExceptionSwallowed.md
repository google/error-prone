This check warns when an `InterruptedException` could be thrown but isn't being
individually handled.

It is important for correctness and performance that thread interruption is
handled properly, however `try` blocks that catch `Exception` or `Throwable` (or
methods that `throws` either type) make it difficult to recognize that
interruption may occur.

For advice on how to handle `InterruptedException`, see https://www.ibm.com/developerworks/library/j-jtp05236/index.html
