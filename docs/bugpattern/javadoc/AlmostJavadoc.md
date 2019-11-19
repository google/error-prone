This comment contains Javadoc or HTML tags, and is in the right position for a
Javadoc, but doesn't start with a double asterisk. Should it be a Javadoc?

```java
/* Frobnicates the {@link Foo}s. */
class Frobnicator {
  Foo frobnicate(Foo foo);
}
```

```java
/** Frobnicates the {@link Foo}s. */
class Frobnicator {
  Foo frobnicate(Foo foo);
}
```

## Suppression

Suppress by applying `@SuppressWarnings("AlmostJavadoc")` to the element being
documented (or not documented).
