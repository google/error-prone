When a field/variable name is the same as the field/variable type, it is
difficult to determine which to use at which time.

For example,

```java
private static String String;
```

This would cause future use of String.something within this class to refer to
the static field String, instead of the class String.

This is worth calling out to avoid confusion and is a violation of
[Google Java style naming conventions](https://google.github.io/styleguide/javaguide.html#s5.2.7-local-variable-names)

Instead of this naming style, the correct way would be:

```java
private static String string;
```
