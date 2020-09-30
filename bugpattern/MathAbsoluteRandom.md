---
title: MathAbsoluteRandom
summary: Math.abs does not always give a positive result. Please consider other methods
  for positive random numbers.
layout: bugpattern
tags: ''
severity: WARNING
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
random positive-or-negative integer (or long), there will a rare edge case where
the returned value will be negative.

This is because there is no positive integer with the same magnitude as
`Integer.MIN_VALUE`, which is equal to `-Integer.MAX_VALUE - 1`. Floating point
numbers don't suffer from this problem, as the sign is stored in a separate bit.

Instead, one should use random number generation functions that are guaranteed
to generate positive numbers:

```java
Random r = new Random();
int positiveNumber = r.nextInt(Integer.MAX_VALUE);
```

or map negative numbers onto the non-negative range:

```java
long lng = r.nextLong();
lng = (lng == Long.MIN_VALUE) ? 0 : Math.abs(lng);
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MathAbsoluteRandom")` to the enclosing element.
