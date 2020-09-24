---
title: ComparisonContractViolated
summary: This comparison method violates the contract
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The comparison contract states that `sgn(compare(x, y)) == -sgn(compare(y, x))`.
(An immediate corollary is that `compare(x, x) == 0`.) This comparison
implementation either a) cannot return 0, b) cannot return a negative value but
may return a positive value, or c) cannot return a positive value but may return
a negative value.

The results of violating this contract can include `TreeSet.contains` never
returning true or `Collections.sort` failing with an IllegalArgumentException
arbitrarily.

In the long term, essentially all Comparators should be rewritten to use the
Java 8 Comparator factory methods, but our automated migration tools will, of
course, only work for correctly implemented Comparators.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparisonContractViolated")` to the enclosing element.
