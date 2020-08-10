---
title: EqualsUsingHashCode
summary: 'Implementing #equals by just comparing hashCodes is fragile. Hashes collide
  frequently, and this will lead to false positives in #equals.'
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Don't implement `#equals` using just a `hashCode` comparison:

```java
class MyClass {
  private final int a;
  private final int b;
  private final String c;

  ...

  @Override
  public boolean equals(@Nullable Object o) {
    return o.hashCode() == hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(a, b, c);
  }
```

The number of `Object`s with randomly distributed `hashCode` required to give a
50% chance of collision (and therefore, with this pattern, erroneously correct
equality) is only ~77k.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsUsingHashCode")` to the enclosing element.
