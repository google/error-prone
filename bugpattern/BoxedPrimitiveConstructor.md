---
title: BoxedPrimitiveConstructor
summary: valueOf provides better time and space performance
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Constructors of primitive wrapper objects (e.g. `new Boolean(true)` will be
[deprecated][120781328] in Java 9. The `valueOf` factory methods 
(e.g. `Boolean.valueOf(true)`) should always be preferred.

[120781328]: https://bugs.openjdk.java.net/browse/JDK-8145468

The explicit constructors are specified to always return a fresh instance, resulting
in unnecessary allocations. The `valueOf` methods return cached
instances for frequently requested values, offering significantly better space
and time performance.

Relying on the unique reference identify of the instances returned by the
explicit constructors is extremely bad practice. Primitives should always be
treated as identity-less value types, even in their boxed representations.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("BoxedPrimitiveConstructor")` annotation to the enclosing element.
