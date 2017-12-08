---
title: FloatingPointLiteralPrecision
summary: Floating point literal loses precision
layout: bugpattern
tags: Style
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`double` and `float` literals that can't be precisely represented should be
avoided.

Example:

```java
double d = 1.9999999999999999999999999999999;
System.err.println(d); // prints 2.0
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FloatingPointLiteralPrecision")` to the enclosing element.
