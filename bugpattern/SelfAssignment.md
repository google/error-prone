---
title: SelfAssignment
summary: Variable assigned to itself
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The left-hand side and right-hand side of this assignment are the same. It has
no effect.

This also handles assignments in which the right-hand side is a call to
Preconditions.checkNotNull(), which returns the variable that was checked for
non-nullity. If you just intended to check that the variable is non-null, please
don't assign the result to the checked variable; just call
Preconditions.checkNotNull() as a bare statement.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SelfAssignment")` to the enclosing element.
