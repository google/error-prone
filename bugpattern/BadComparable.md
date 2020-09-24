---
title: BadComparable
summary: Possible sign flip from narrowing conversion
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A narrowing integral conversion can cause a sign flip, since it simply discards
all but the n lowest order bits, where n is the number of bits used to represent
the target type (JLS 5.1.3). In a compare or compareTo method, this can cause
incorrect and unstable sort orders.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BadComparable")` to the enclosing element.
