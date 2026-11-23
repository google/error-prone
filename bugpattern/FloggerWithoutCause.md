---
title: FloggerWithoutCause
summary: Use withCause to associate Exceptions with log statements
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Flogger uses `withCause(exception)` to associate Exceptions with log statements.
Passing exceptions directly to `log()` only records the name and message, and
loses the stack trace.

```java
logger.atWarning().log("Unexpected exception: %s", e);
```

```java
logger.atWarning().withCause(e).log("Unexpected exception");
```

If you intended not to log the stack trace or other parts of the exception, you
should explicitly log the parts you want to keep so the intent is clear in the
code:

```java
// Avoid withCause() since stack traces are unnecessary here
logger.atWarning().log("Unexpected exception [%s]: %s", e.getClass(), e.getMessage());
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerWithoutCause")` to the enclosing element.
