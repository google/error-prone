---
title: EqualsWrongThing
summary: Comparing different pairs of fields/getters in an equals implementation is probably a mistake.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
An equals method compares non-corresponding fields from itself and the other
instance:

```java {.bad}
class Frobnicator {
  private int a;
  private int b;

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof Frobnicator)) {
      return false;
    }
    Frobnicator that = (Frobnicator) other;
    return a == that.a && b == that.a; // BUG: should be b == that.b
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsWrongThing")` to the enclosing element.
