Methods like Futures.whenAllComplete(...).callAsync(...) will throw a
NullPointerException if the provided AsyncCallable returns a null Future. To
produce a Future with an output of null, instead return immediateFuture(null).
