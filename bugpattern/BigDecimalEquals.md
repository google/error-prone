---
title: BigDecimalEquals
summary: 'BigDecimal#equals has surprising behavior: it also compares scale.'
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
`BigDecimal`'s equals method compares the scale of the representation as well as
the numeric value, which may not be expected.

```java {.bad}
BigDecimal a = new BigDecimal("1.0");
BigDecimal b = new BigDecimal("1.00");
a.equals(b); // false!
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BigDecimalEquals")` to the enclosing element.
