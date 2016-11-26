The JSR 305 `@CheckReturnValue` annotation marks methods whose return values
should be checked. This error is triggered when one of these methods is called
but the result is not used.

`@CheckReturnValue` may be applied to a class or package to indicate that all
methods in that class or package must have their return values checked. For
convenience, we provide an annotation, `@CanIgnoreReturnValue`, to exempt
specific methods or classes from this behavior. `@CanIgnoreReturnValue` is
available from the Error Prone annotations package,
`com.google.errorprone.annotations`.

If you really want to ignore the return value of a method annotated with
`@CheckReturnValue`, a cleaner alternative to `@SuppressWarnings` is to assign
the result to a variable that starts with `unused`:

```java
public void setNameFormat(String nameFormat) {
  String unused = format(nameFormat, 0); // fail fast if the format is bad or null
  this.nameFormat = nameFormat;
}
```

NOTE: `@CheckReturnValue` is ignored under the following conditions (which saves
users from having to use either an `unused` variable or `@SuppressWarnings`):

1.  calls from `Mockito.verify()`; e.g., `Mockito.verify(t).foo()` (where
    `foo()` is annotated with `@CheckReturnValue`)

2.  calls from `Stubber.when()`; e.g. `doReturn(val).when(t).foo()` (where
    `foo()` is annotated with `@CheckReturnValue`)

3.  code that is using the `try/execute/fail/catch` pattern; e.g.:

```java
try {
  user.setName(null);
  fail("Expected a NullPointerException to be thrown on a null name");
} catch (NullPointerException expected) {
}
```

This is because such tests meant to check if a method is invoked and/or throws
the correct exception type, rather than consuming the return value.

