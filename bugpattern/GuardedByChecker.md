---
title: GuardedByChecker
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

# Bug pattern: GuardedByChecker
__Checks for unguarded accesses to fields and methods with @GuardedBy annotations__

## The problem
The @GuardedBy annotation is used to associate a lock with a fields or methods. Accessing a guarded field or invoking a guarded method should only be done when the specified lock is held. Unguarded accesses are not thread safe.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("GuardedByChecker")` annotation to the enclosing element.
