---
title: EqualsUnsafeCast
summary: 'The contract of #equals states that it should return false for incompatible
  types, while this implementation may throw ClassCastException.'
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Implementations of `#equals` should return `false` for different types, not
throw.

```java {.bad}
class Data {
  private int a;

  @Override
  public boolean equals(Object other) {
    Data that = (Data) other; // BAD: This may throw ClassCastException.
    return a == that.a;
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsUnsafeCast")` to the enclosing element.
