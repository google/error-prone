APIs that accept a `java.time.Duration` or `java.time.Instant` should be
preferred, when available.

JodaTime is now considered a legacy library for Java 8+ users.

Representing date/time concepts as numeric primitives is strongly discouraged
(e.g., `long timeout`).

APIs that require a `long, TimeUnit` pair suffer from a number of problems

1.  they may require plumbing 2 parameters through various layers of your
    application

2.  overflows are possible when doing duration math

3.  they lack semantic meaning (when viewed separately)

4.  decomposing a duration into a `long, TimeUnit` is dangerous because of unit
    mismatch and/or excessive truncation.
