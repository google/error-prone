Discarding an exception after calling `printStackTrace` should usually be
avoided.


```java {.good}
try {
  // ...
} catch (IOException e) {
  logger.log(INFO, "something has gone terribly wrong", e);
}
```

```java {.good}
try {
  // ...
} catch (IOException e) {
  throw new UncheckedIOException(e); // New in Java 8
}
```

```java {.bad}
try {
  // ...
} catch (IOException e) {
  e.printStackTrace();
}
```
