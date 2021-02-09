Prefer string formatting to concatenating format arguments together, to avoid
work at the log site.

That is, prefer this:

```java
logger.atInfo().log("processing: %s", request);
```

to this, which calls `request.toString()` even if `INFO` logging is disabled:

```java
logger.atInfo().log("processing: " + request);
```

More information: https://google.github.io/flogger/formatting
