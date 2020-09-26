---
title: HashCodeToString
summary: Classes that override hashCode should also consider overriding toString.
layout: bugpattern
tags: FragileCode
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Classes that override `hashCode()` should consider also overriding `toString()`
with details to aid debugging and diagnostics, instead of relying on the default
`Object.toString()` implementation.

`Object.toString()` returns a string consisting of the class' name and the
instances' hash code. When `hashCode()` is overridden this can be misleading, as
users typically expect this default `toString()` to be (semi)unique
per-instance, especially when debugging.

See also
[`MoreObjects.toStringHelper()`](https://guava.dev/releases/snapshot/api/docs/com/google/common/base/MoreObjects.html#toStringHelper-java.lang.Object-)

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("HashCodeToString")` to the enclosing element.
