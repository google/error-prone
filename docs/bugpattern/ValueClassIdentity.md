[Value-based Classes][value-based-classes] should not be used with operations
that rely on object identity including reference equality, identity hashing,
synchronization, or serialization.

[JEP 401][jep-401] introduces Value Classes and Objects as a preview feature in
Java 28. Many existing JDK types (such as primitive wrappers, `Optional`, and
`java.time` classes) are value-based and are migrating to become true value
classes.

Operations to avoid on value classes:

Synchronization and Monitors
:   Value objects do not have an object monitor. Synchronizing on them or
    calling `wait()`, `notify()`, or `notifyAll()` cannot provide mutual
    exclusion (as the runtime may freely copy or recreate instances) and throws
    `IdentityException`.

Weak, Soft, and Identity References
:   Classes like `WeakReference`, `SoftReference`, and `PhantomReference` track
    the lifecycle of a specific object instance in memory. Because value objects
    can be flattened, inlined, or rematerialized by the JVM, tracking their
    lifetime is meaningless.

Identity-Based Maps and Caches
:   `IdentityHashMap`, `WeakHashMap`, and builder configurations like
    `CacheBuilder.weakKeys()`, `CacheBuilder.weakValues()`,
    `MapMaker.weakKeys()`, or `Caffeine.weakKeys()` rely on reference identity
    (`==`) or reference queues. Value classes will not behave correctly with
    identity lookup or weak eviction.

`System.identityHashCode(...)`
:   `System.identityHashCode` computes a hash code based on object identity.
    Value-based classes do not have persistent identity, so calling
    `System.identityHashCode` on them is not equivalent to `val.hashCode()` and
    produces unstable results across equal or re-boxed instances. Use
    `Objects.hashCode(val)` or `val.hashCode()` instead.

[value-based-classes]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/doc-files/ValueBased.html
[jep-401]: https://openjdk.org/jeps/401
