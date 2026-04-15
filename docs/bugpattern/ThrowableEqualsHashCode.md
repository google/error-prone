Throwables should not override `equals()` or `hashCode()`.

1.  **Exceptions are Events, Not Value Objects** Philosophically, an exception
    represents a unique historical event: something went wrong at a specific
    time, in a specific thread, at a specific line of code. It is not a data
    container or a value type (like a `String` or a `Money` object). Even if two
    `IllegalArgumentException`s are thrown with the exact same message (`"ID
    cannot be null"`), and perhaps even identical stack traces, they represent
    two distinct failures that happened independently. Treating them as "equal"
    conceptually conflates two different events.

2.  **The Stack Trace Problem** When an exception is instantiated (or thrown),
    Java populates its stack trace via `fillInStackTrace()`.

    *   Complexity: Are you going to include the stack trace in your `equals()`
        comparison? If you do, comparing arrays of `StackTraceElement` is
        computationally expensive. Exceptions can also have causes and
        suppressed exceptions. This adds expense, too. Also, will all the
        transitive causes and suppressed exceptions themselves implement
        `equals()` the way you want? Plus, causes and suppressed exceptions make
        exceptions mutable, and mutable objects generally shouldn't implement
        `equals()`.
    *   Brittleness: If you don't include the stack trace, you are saying that
        an exception thrown in `ServiceA` is equal to an exception thrown in
        `ServiceB` just because they share a message or an error code. This
        masks critical debugging context.

3.  **It Hides Bad Architecture** The primary reason you would need to override
    these methods is if you are placing Exceptions into a `HashSet`, or using
    them as keys in a `HashMap`. If you are doing this, you are likely using
    Exceptions for normal business logic or control flow, which is a known
    anti-pattern. Exceptions are for exceptional circumstances; they are heavy
    (because of the stack trace) and slow to generate.
