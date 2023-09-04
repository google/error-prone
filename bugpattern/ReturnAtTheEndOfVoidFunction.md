---
title: ReturnAtTheEndOfVoidFunction
summary: '`return;` is unnecessary at the end of void methods and constructors.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Detects no-op `return` statements in `void` functions when they occur at the end
of the method.

Instead of:

```java
public void stuff() {
  int x = 5;
  return;
}
```

do:

```java
public void stuff() {
  int x = 5;
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ReturnAtTheEndOfVoidFunction")` to the enclosing element.
