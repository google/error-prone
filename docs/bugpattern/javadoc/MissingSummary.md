Javadocs on private and protected elements are required to contain a short
summary line. This is often the only part of the Javadoc that others will see
surfaced in various tools.

```java {.bad}
/** @return deserialised proto */
public Proto parse(byte[] bytes) {
  ...
}
```

```java {.good}
/** Returns deserialised proto. */
public Proto parse(byte[] bytes) {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("MissingSummary")` to the element being
documented.
