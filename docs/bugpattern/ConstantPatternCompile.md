Method bodies should generally not call
`java.util.regex.Pattern#compile(String)` with constant arguments. Instead,
define a constant to store that Pattern. This can avoid recompilation of the
regex every time the method is invoked.

That is, prefer this:

```java
private static final Pattern REGEX_PATTERN = Pattern.compile("a+");

public static boolean doSomething(String input) {
  Matcher matcher = REGEX_PATTERN.matcher(input);
  if (matcher.matches()) {
    ...
  }
}
```

to this:

```java
public static boolean doSomething(String input) {
  Matcher matcher = Pattern.compile("a+").matcher(input);
  if (matcher.matches()) {
    ...
  }
}
```
