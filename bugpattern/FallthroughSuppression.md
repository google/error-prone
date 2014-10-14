---
title: FallthroughSuppression
layout: bugpattern
category: ONE_OFF
severity: NOT_A_PROBLEM
maturity: EXPERIMENTAL
---

# Bug pattern: FallthroughSuppression
__Fallthrough warning suppression has no effect if warning is suppressed__

## The problem
Remove all arguments to @SuppressWarnings annotations that suppress the Java compiler's fallthrough warning. If there are no more arguments in a @SuppressWarnings annotation, remove the whole annotation.

Note: This checker was specific to a refactoring we performed and should not be used as a general error or warning.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("FallthroughSuppression")` annotation to the enclosing element.
