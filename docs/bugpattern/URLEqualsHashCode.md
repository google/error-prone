The `equals` and `hashCode` methods of `java.net.URL` make blocking network
calls. When you place a `URL` into a hash-based container, the container invokes
those methods.

Prefer `java.net.URI`. Or, if you must use `URL` in a
collection, prefer to use a non-hash-based container like a `List<URL>`, and
avoid calling methods like `contains` (which calls `equals`) on it.
