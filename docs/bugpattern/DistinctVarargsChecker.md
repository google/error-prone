Various methods which take variable-length arguments throw runtime exceptions
like `IllegalArgumentException` when the arguments are not distinct.

This checker warns on using non-distinct parameters in various varargs methods
when the usage is either redundant or will result in a runtime exception.

Bad:

```java
ImmutableSet.of(first, second, second, third);
```

Good:

```java
ImmutableSet.of(first, second, third);
```
