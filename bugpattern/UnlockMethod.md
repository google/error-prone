---
title: UnlockMethod
summary: "This method does not acquire the locks specified by its @UnlockMethod annotation"
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: GuardedBy_

## The problem
Methods with the @UnlockMethod annotation are expected to release one or more locks. The caller must hold the locks when the function is entered, and will not hold them when it completes.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("UnlockMethod")` annotation to the enclosing element.
