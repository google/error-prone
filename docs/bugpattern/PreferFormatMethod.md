When calling a method or constructor that has an overload annotated with
`@FormatMethod`, prefer passing the format string and format arguments directly
to the `@FormatMethod` overload rather than pre-formatting the message with
`String.format(...)` or `"..." .formatted(...)`.

Calling the `@FormatMethod` overload directly is more concise, avoids redundant
wrapping, and allows APIs that format lazily (such as precondition or logging
helpers) to defer string formatting until needed.

```java
throw new MyException(String.format("Invalid request %s for user %d", reqId, userId));
throw new MyException("Invalid request %s".formatted(reqId), cause);
```

```java
throw new MyException("Invalid request %s for user %d", reqId, userId);
throw new MyException(cause, "Invalid request %s", reqId);
```
