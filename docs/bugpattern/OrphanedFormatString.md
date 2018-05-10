Passing a string that contains format specifiers to a method that does not
perform string formatting is usually a mistake.

Do this:

```java {.good}
if (!isValid(arg)) {
  throw new IllegalArgumentException(String.format("invalid arg: %s", arg));
}
```

Not this:

```java {.bad}
if (!isValid(arg)) {
  throw new IllegalArgumentException("invalid arg: %s");
}
```

If the method you're calling actually accepts a format string, you can annotate
that method with [`@FormatMethod`][fm] to ensure that callers correctly pass
format strings (and to inform Error Prone that the method call you're making
doesn't orphan a format string).

[fm]: https://errorprone.info/api/latest/com/google/errorprone/annotations/FormatMethod.html
