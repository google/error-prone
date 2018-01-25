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
