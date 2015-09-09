The JSR 305 `@CheckReturnValue` annotation marks methods whose return values
should be checked.  This error is triggered when one of these methods is called
but the result is not used.

`@CheckReturnValue` may be applied to a class or package to indicate that all
methods in that class or package must have their return values checked.  For
convenience, we provide an annotation, `@CanIgnoreReturnValue`, to exempt
specific methods or classes from this behavior.  `@CanIgnoreReturnValue` is
available from the Error Prone annotations package,
`com.google.errorprone.annotations`.

If you really want to ignore the return value of a method annotated with
`@CheckReturnValue`, a cleaner alternative to `@SuppressWarnings` is to assign
the result to a variable named `unused`:

```java
public void setNameFormat(String nameFormat) {
  String unused = format(nameFormat, 0); // fail fast if the format is bad or null
  this.nameFormat = nameFormat;
}
```

