---
title: NarrowCalculation
summary: This calculation may lose precision compared to its target type.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Integer division is suspicious when the target type of the expression is a
float. For example:

```java
private static final double ONE_HALF = 1 / 2; // Actually 0.
```

If you specifically want the integer result, consider pulling out variable to
make it clear what's happening. For example, instead of:

```java
// Deduct 10% from the grade for every week the assignment is late:
float adjustedGrade = grade - (days / 7) * .1;
```

Prefer:

```java
// Deduct 10% from the grade for every week the assignment is late:
int fullWeeks = days / 7;
float adjustedGrade = grade - fullWeeks * .1;
```

Similarly, multiplication of two `int` values which are then cast to a `long` is
problematic, as the `int` multiplication could overflow. It's better to perform
the multiplication using `long` arithmetic.

```java
long secondsToNanos(int seconds) {
  return seconds * 1_000_000_000; // Oops; starts overflowing around 2.15 seconds.
}
```

Instead, prefer:

```java
long secondsToNanos(int seconds) {
  return seconds * 1_000_000_000L; // Or ((long) seconds) * 1_000_000_000.
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NarrowCalculation")` to the enclosing element.
