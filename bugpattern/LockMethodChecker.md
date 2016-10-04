---
title: LockMethodChecker
summary: This method does not acquire the locks specified by its @LockMethod annotation
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: GuardedBy_

## The problem
Methods with the @LockMethod annotation are expected to acquire one or more locks. The caller will hold the locks when the function finishes execution.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("LockMethodChecker")` annotation to the enclosing element.
