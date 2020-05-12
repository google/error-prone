Prefer to use literal format strings directly in the call to a formatting method
over extracting them to a constant. That is, prefer this:

```java
throw new IllegalArgumentException(
    String.format("Uh oh, can't use %s with %s", badArgA, badArgB));
```

to this:

```java
private static final String ERROR_MESSAGE = "Uh oh, can't use %s with %s";
...
throw new IllegalArgumentException(String.format(ERROR_MESSAGE, badArgA, badArgB));
```

Extracting the format string to a constant makes it harder to read the
`String.format` call, and to see that the correct format arguments are being
passed.

If a single format string is used by multiple calls to `String.format`, consider
extracting a helper method instead of making the string a constant:

```
String errorMessage(String badArgA, String badArgB) {
  return String.format("Uh oh, can't use %s with %s", badArgA, badArgB);
}
...
throw new IllegalArgumentException(errorMessage(badArgA, badArgB));
```
