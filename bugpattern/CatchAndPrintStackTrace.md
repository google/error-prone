---
title: CatchAndPrintStackTrace
summary: Logging or rethrowing exceptions should usually be preferred to catching and calling printStackTrace
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
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
  throw new UncheckedIOException(e);
}
```

```java {.bad}
try {
  // ...
} catch (IOException e) {
  e.printStackTrace();
}
```

If you truly intend to print a stack trace to stderr, do so explicitly:

```java
e.printStackTrace(System.err);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CatchAndPrintStackTrace")` to the enclosing element.
