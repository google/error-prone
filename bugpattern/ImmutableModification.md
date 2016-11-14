---
title: ImmutableModification
summary: Modifying an immutable collection is guaranteed to throw an exception and leave the collection unmodified
layout: bugpattern
category: GUAVA
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Calling a method that modifies a collection on an immutable implementation (e.g. `ImmutableList.add`) is guaranteed to always throw an `UnsupportedOperationException` and leave the collection unmodified.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ImmutableModification")` annotation to the enclosing element.
