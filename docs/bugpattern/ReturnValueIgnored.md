Certain library methods do nothing useful if their return value is ignored. For
example, String.trim() has no side effects, and you must store the return value
of String.intern() to access the interned string. This check encodes a list of
methods in the JDK whose return value must be used and issues an error if they
are not.

## `Optional.orElseThrow` {#orElseThrow}

Don't call `orElseThrow` just for its side-effects. When the result of a call to
`orElseThrow` is discarded, it may be unclear to future readers whether the
result is being discarded deliberately or accidentally. That is, avoid:

```java
// return value of orElseThrow() is silently ignored here
optional.orElseThrow(() -> new AssertionError("something has gone terribly wrong"));
```

Instead of calling `orElseThrow` for its side-effects, prefer an explicit call
to `isPresent()`, or use one of `checkState` or `checkArgument` from
[Guava's `Preconditions` class](https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/base/Preconditions.html).

```java
if (!optional.isPresent()) {
  throw new AssertionError("something has gone terribly wrong");
}
```

``` {.good}
checkState(optional.isPresent(), "something has gone terribly wrong");
```

``` {.good}
checkArgument(optional.isPresent(), "something has gone terribly wrong");
```
