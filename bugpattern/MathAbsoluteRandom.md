---
title: MathAbsoluteRandom
summary: Math.abs does not always give a positive result. Please consider other methods for positive random numbers.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
[`Math.abs`](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#abs-long-)
returns a negative number when called with the largest negative number.

Example:

```java
int veryNegative = Math.abs(Integer.MIN_VALUE);
long veryNegativeLong = Math.abs(Long.MIN_VALUE);
```

When trying to generate positive random numbers by using `Math.abs` around a
random positive-or-negative number, there will be (very infrequent) occasions
where the random number will be negative.

Instead, one should use random number generation functions that are guaranteed
to generate positive numbers:

```java
Random r = new Random();
int positiveNumber = r.nextInt(Integer.MAX_VALUE);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MathAbsoluteRandom")` to the enclosing element.
