---
title: NullTernary
summary: This conditional expression may evaluate to null, which will result in an NPE when the result is unboxed.
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
If a conditional expression evalutes to `null`, unboxing it will result in a
`NullPointerException`.

For example:

```java
int x = flag ? foo : null:
```

If `flag` is false, `null` will be auto-unboxed from an `Integer` to `int`,
resulting in a NullPointerException.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullTernary")` to the enclosing element.
