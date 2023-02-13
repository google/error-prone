Java allows methods to declare that they throw checked exceptions even when they don't
(including unthrown exceptions can be reasonable where the method is overridable,
as overriding methods will not be able to declare that they throw any exceptions
not included.) However, this can lead to call sites being forced to explicitly handle or
propagate exceptions which provably can never occur.

```java
private static void validateRequest(Request request) throws IOException {
  if (!request.hasFoo()) {
    throw new IllegalArgumentException("foo must be specified");
  }
}

Response handle(Request request) {
  try {
    validateRequest(request);
  } catch (IOException e) { // Required, but unreachable.
    return failedResponse();
  }
  // ...
}
```

The check will not flag overridable methods.
