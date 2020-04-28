Suppressions for `unchecked` or `rawtypes` warnings should have an accompanying
comment to explain why the javac warning is safe to ignore.

Rather than just suppressing the warning:

```java
@SuppressWarnings("unchecked")
public ImmutableList<Object> performScaryCast(ImmutableList<String> strings) {
  return (ImmutableList<Object>) (ImmutableList<?>) strings;
}
```

Provide a concise explanation for why it is safe:

```java
@SuppressWarnings("unchecked") // Safe covariant cast, given ImmutableList cannot be added to.
public ImmutableList<Object> performScaryCast(ImmutableList<String> strings) {
  return (ImmutableList<Object>) (ImmutableList<?>) strings;
}
```
