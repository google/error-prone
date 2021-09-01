Starting in Java 9, the resource in a try-with-resources statement can be a
reference to a `final` or effectively-`final` variable.

That is, you can write this:

```java
AutoCloseable resource = ...;
try (resource) {
  doSomething(resource);
}
```

instead of this:

```java
AutoCloseable resource = ...;
try (AutoCloseable resource2 = resource) {
  doSomething(resource2);
}
```

NOTE: the resource cannot be an arbitrary expression, for example `try
(returnsTheResources()) { ... }` is still not allowed.
