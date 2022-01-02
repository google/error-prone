---
title: RedundantOverride
summary: This overriding method is redundant, and can be removed.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
An override of a method that delegates its implementation to the super method is
redudant, and can be removed.

For example, the `equals` method in the following class implementation can be
deleted.

```java
class Test {
  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RedundantOverride")` to the enclosing element.
