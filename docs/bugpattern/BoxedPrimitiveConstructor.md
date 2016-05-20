Constructors of primitive wrapper objects (e.g. `new Boolean(true)` will be
[deprecated][8145468] in Java 9. The `valueOf` factory methods
(e.g. `Boolean.valueOf(true)`) should always be preferred. Those methods are
called implicitly by autoboxing, which is often more convenient than an
explicit call. `Integer x = Integer.valueOf(23);` and `Integer x = 23;` are
equivalent.

[8145468]: https://bugs.openjdk.java.net/browse/JDK-8145468

The explicit constructors always return a fresh instance, resulting
in unnecessary allocations. The `valueOf` methods return cached
instances for frequently requested values, offering significantly better space
and time performance.

Relying on the unique reference identity of the instances returned by the
explicit constructors is extremely bad practice. Primitives should always be
treated as identity-less value types, even in their boxed representations.
