Some methods and constructors require that the returned resource is closed. This
includes user methods/constructors annotated with `@MustBeClosed` and some
additional well-known APIs. (Some further well-known APIs are covered by a
different check, StreamResourceLeak.)

This is enforced by checking that invocations occur within the resource variable
initializer of a try-with-resources statement:

```java
try (AutoCloseable resource = createTheResource()) {
  doSomething(resource);
}
```

or the `return` statement of another method annotated with `@MustBeClosed`:

```java
@MustBeClosed
AutoCloseable createMyResource() {
  return createTheResource();
}
```

To support legacy code, the following pattern is also supported:

```java
AutoCloseable resource = createTheResource();
try {
  doSomething(resource);
} finally {
  resource.close();
}
```
