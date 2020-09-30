---
title: FloatingPointAssertionWithinEpsilon
summary: This fuzzy equality check is using a tolerance less than the gap to the next
  number. You may want a less restrictive tolerance, or to assert equality.
layout: bugpattern
tags: Simplification
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Both JUnit and Truth allow for asserting equality of floating point numbers with
an absolute tolerance. For example, the following statements are equivalent,

```java
double EPSILON = 1e-20;
assertThat(actualValue).isWithin(EPSILON).of(Math.PI);
assertEquals(Math.PI, actualValue, EPSILON);
```

What's not immediately obvious is that both of these assertions are checking
exact equality between `Math.PI` and `actualValue`, because the next `double`
after `Math.PI` is `Math.PI + 4.44e-16`.

This means that using the same tolerance to compare several floating point
values with different magnitude can be prone to error,

```java
float TOLERANCE = 1e-5f;
assertThat(pressure).isWithin(TOLERANCE).of(1f); // GOOD
assertThat(pressure).isWithin(TOLERANCE).of(10f); // GOOD
assertThat(pressure).isWithin(TOLERANCE).of(100f); // BAD -- misleading equals check
```

A larger tolerance should be used if the goal of the test is to allow for some
floating point errors, or, if not, `isEqualTo` makes the intention more clear.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloatingPointAssertionWithinEpsilon")` to the enclosing element.
