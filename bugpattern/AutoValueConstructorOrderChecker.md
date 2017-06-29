---
title: AutoValueConstructorOrderChecker
summary: Arguments to AutoValue constructor are in the wrong order
layout: bugpattern
category: GUAVA
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
AutoValue constructors are synthesized with their parameters in the same order as the abstract accessor methods. Calls to the constructor need to match this ordering.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("AutoValueConstructorOrderChecker")` annotation to the enclosing element.
