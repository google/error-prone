When using a logger as error handling, setting the exception as the cause of the
log statement will provide more context in the log message.

Instead of:

```java
try {
  ...
} catch (Exception e) {
  logger.atWarning().log("Failed!");
}
```

Consider:

```java
try {
  ...
} catch (Exception e) {
  logger.atWarning().withCause(e).log("Failed!");
}
```
