---
title: CompareToZero
summary: 'The result of #compareTo or #compare should only be compared to 0. It is
  an implementation detail whether a given type returns strictly the values {-1, 0,
  +1} or others.'
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
The contract for `Comparator#compare` and `Comparable#compareTo` states that the
result is an integer which is `< 0` for less than, `== 0` for equality and `> 0`
for greater than. While most implementations return `-1`, `0` and `+1` for those
cases respectively, this is not guaranteed. Always comparing to `0` is the
safest use of the return value.

```java {.bad}
  boolean <T> isLessThan(Comparator<T> comparator, T a, T b) {
    // Fragile: it's not guaranteed that `comparator` returns -1 to mean
    // "less than".
    return comparator.compare(a, b) == -1;
  }
```

```java {.good}
  boolean <T> isLessThan(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) < 0;
  }
```

Even comparisons which are otherwise correct are significantly clearer to other
readers of the code if turned into a comparison to `0`, e.g.:

```java
  boolean <T> greaterOrEqual(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) > 1;
  }
```

```java {.good}
  boolean <T> greaterOrEqual(Comparator<T> comparator, T a, T b) {
    return comparator.compare(a, b) >= 0;
  }
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CompareToZero")` to the enclosing element.
