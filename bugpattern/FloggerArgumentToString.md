---
title: FloggerArgumentToString
summary: Use Flogger's printf-style formatting instead of explicitly converting arguments
  to strings
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerArgumentToString")` to the enclosing element.
