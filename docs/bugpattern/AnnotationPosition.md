Per the [style guide](style-guide), `TYPE_USE` annotations should appear
immediately before the type being annotated, and after any modifiers:

```java
public <K, V> @Nullable V getOrNull(final Map<K, V> map, final @Nullable K key) {
  return map.get(key);
}
```

Non-`TYPE_USE` annotations should appear before modifiers, as they annotate the
entire element (method, variable, class):

```java
@VisibleForTesting
public void reset() {
  // ...
}
```

Javadoc must appear before any annotations, or the compiler will fail to
recognise it as Javadoc:

```java
@Nullable
/** Might return a frobnicator. */
Frobnicator getFrobnicator();
```

[style-guide]: https://google.github.io/styleguide/javaguide.html#s4.8.5.1-type-use-annotation-style
