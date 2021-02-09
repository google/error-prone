---
title: FloggerStringConcatenation
summary: Prefer string formatting using printf placeholders (e.g. %s) instead of string
  concatenation
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
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

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloggerStringConcatenation")` to the enclosing element.
