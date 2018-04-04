Methods like Futures.transformAsync and Futures.catchingAsync will throw a
NullPointerException if the provided AsyncFunction returns a null Future. To
produce a Future with an output of null, instead return immediateFuture(null).
