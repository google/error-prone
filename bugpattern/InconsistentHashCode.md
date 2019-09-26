---
title: InconsistentHashCode
summary: Including fields in hashCode which are not compared in equals violates the contract of hashCode.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Implementations of `Object#hashCode` should not incorporate fields which the
implementation of `Object#equals` does not. This violates the contract of
`hashCode`: specifically, equal objects must have equal hashCodes.

```java
class Foo {
  private final int a;
  private final int b;

  Foo(int a, int b) {
    this.a = a;
    this.b = b;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return o instanceof Foo && ((Foo) o).a == a;
  }

  @Override
  public int hashCode() {
    return a + 31 * b;
  }
}

Foo first = new Foo(10, 20);
Foo second = new Foo(10, 40);

first.equals(second) // true
first.hashCode() == second.hashCode() // false
```

The fix for this class is either to include a comparison of `b` in the `#equals`
method, or remove `b` from `#hashCode`. The former is more likely to be correct.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InconsistentHashCode")` to the enclosing element.
