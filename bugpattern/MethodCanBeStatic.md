---
title: MethodCanBeStatic
summary: Private methods that do not reference the enclosing instance should be static
layout: bugpattern
category: JDK
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Any private helper method that never accesses `this` (even implicitly) is
already static in spirit. Adding an explicit static keyword makes it clear that
the method makes no use of instance state, and prevents a future editor from
doing so accidentally.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MethodCanBeStatic")` annotation to the enclosing element.
