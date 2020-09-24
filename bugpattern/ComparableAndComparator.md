---
title: ComparableAndComparator
summary: Class should not implement both `Comparable` and `Comparator`
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A `Comparator` is an object that knows how to compare other objects, whereas an
object implementing `Comparable` knows how to compare itself to other objects of
the same type.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparableAndComparator")` to the enclosing element.
