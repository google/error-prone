---
title: MissingSummary
summary: A summary line is required on public/protected Javadocs.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Javadocs on private and protected elements are required to contain a short
summary line. This is often the only part of the Javadoc that others will see
surfaced in various tools.

```java
/** @return deserialised proto */
public Proto parse(byte[] bytes) {
  ...
}
```

```java
/** Returns deserialised proto. */
public Proto parse(byte[] bytes) {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("MissingSummary")` to the element being
documented.

