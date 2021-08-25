Prefer to let Flogger transform your arguments to strings, instead of calling
`toString()` explicitly.

For example, prefer the following:

```java
logger.atInfo().log("hello '%s'", world);
```

instead of this, which eagerly calls `world.toString()` even if `INFO` level
logging is disabled.

```java
logger.atInfo().log("hello '%s'", world.toString());
```
