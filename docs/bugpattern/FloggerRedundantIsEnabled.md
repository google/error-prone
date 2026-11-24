Guarding flogger log statements with an explicit log level check is redundant,
since the log statement already contains the desired log level and is
inexpensive to evaluate if the specified log level is disabled.

This is redundant:

```java
if (logger.atInfo().isEnabled()) {
   logger.atInfo().log(\"blah\");
}
```

If the log statement's *arguments* are expensive to evaluate, consider using
lazy argument evaluation
(https://google.github.io/flogger/examples#logging-with-lazy-argument-evaluation-java8):

```java
logger.atFine().log(\"Value: %s\", lazy(() -> process(x, y)));
```
