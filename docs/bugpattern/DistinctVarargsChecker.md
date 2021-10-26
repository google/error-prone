Various methods which take variable-length arguments throw the runtime
exceptions like `IllegalArgumentException` when the arguments are not distinct.

This checker warns on using the non-distinct parameters in various varargs
method when the usage is redundant or will either result in the runtime
exception.

Bad:

```java
ImmutableSet.of(first, second, second, third);
```

Good:

```java
ImmutableSet.of(first, second, third);
```
