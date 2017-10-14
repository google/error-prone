---
title: IntLongMath
summary: Expression of type int may overflow before being assigned to a long
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Performing an arithmetic expression on arguments of type int and then assigning
the result to a long is error-prone. The result is widened to a long as the
final step, and the intermediate results may overflow.

For example, the following expression exceeds `Integer.MAX_VALUE` and overflows
to `-1857093632`:

```java
long nanosPerDay = 24 * 60 * 60 * 1000 * 1000 * 1000;
```

The corrected code (which has a value of `86400000000000`) is:

```java
long nanosPerDay = 24L * 60 * 60 * 1000 * 1000 * 1000;
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("IntLongMath")` annotation to the enclosing element.
