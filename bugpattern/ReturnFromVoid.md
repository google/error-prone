---
title: ReturnFromVoid
summary: Void methods should not have a @return tag.
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Methods which do not return anything should not have a `@return` tag.

```java
/**
 * Frobnicates.
 *
 * @return a frobnicator
 */
void frobnicator(int a) {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("ReturnFromVoid")` to the element being
documented.

