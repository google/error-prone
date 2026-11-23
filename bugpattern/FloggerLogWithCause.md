---
title: FloggerLogWithCause
summary: Setting the caught exception as the cause of the log message may provide
  more context for anyone debugging errors.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerLogWithCause")` to the enclosing element.
