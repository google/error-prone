---
title: ReferenceEquality
summary: Comparison using reference equality instead of value equality
layout: bugpattern
category: JDK
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Reference types that declare an `equals()` method, or that inherit `equals()`
from a type other than `Object`, should not be compared for reference equality
using `==` or `!=`. Instead, compare for value equality using `.equals()`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ReferenceEquality")` annotation to the enclosing element.
