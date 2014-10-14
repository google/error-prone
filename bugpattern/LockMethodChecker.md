---
title: LockMethodChecker
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

# Bug pattern: LockMethodChecker
__This method does not acquire the locks specified by its @LockMethod annotation__
  * Alternate names: GuardedBy
## The problem
Methods with the @LockMethod annotation are expected to acquire one or more locks. The caller will hold the locks when the function finishes execution.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("LockMethodChecker")` annotation to the enclosing element.
