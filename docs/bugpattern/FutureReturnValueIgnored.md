Methods that return `java.util.concurrent.Future` and its subclasses generally
indicate errors by returning a future that eventually fails.

If you don’t check the return value of these methods, you will never find out if
they threw an exception.

Nested futures can also result in missed cancellation signals or suppressed
exceptions - see
[Avoiding Nested Futures](https://github.com/google/guava/wiki/ListenableFutureExplained#avoid-nested-futures)
for details.
