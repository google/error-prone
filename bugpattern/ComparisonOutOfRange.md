---
title: ComparisonOutOfRange
summary: Comparison to value that is out of range for the compared type
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This checker looks for equality comparisons to values that are out of range for the compared type.  For example, bytes may have a value in the range -128 to 127. Comparing a byte for equality with a value outside that range will always evaluate to false and usually indicates an error in the code.

This checker currently supports checking for bad byte and character comparisons.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ComparisonOutOfRange")` to the enclosing element.
