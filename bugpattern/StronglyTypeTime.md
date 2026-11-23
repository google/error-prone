---
title: StronglyTypeTime
summary: This primitive integral type is only used to construct time types. It would
  be clearer to strongly type the field instead.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Where possible, `java.time.Duration` fields should be strongly typed rather than
stored as integral primitives and converted at the usage site.

For example, rather than:

```java
public class X {
  private static final long TIMEOUT = 100;

  // later in the file
  use(Duration.ofMillis(TIMEOUT));
}
```

Prefer:

```java
public class X {
  private static final Duration TIMEOUT = Duration.ofMillis(100);

  // later in the file
  use(TIMEOUT);
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StronglyTypeTime")` to the enclosing element.
