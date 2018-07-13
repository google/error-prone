---
title: AutoValueFinalMethods
summary: Make toString(), hashCode() and equals() final in AutoValue classes, so it is clear to readers that AutoValue is not overriding them
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Consider that other developers will try to read and understand your value class
while looking only at your hand-written class, not the actual (generated)
implementation class. If you mark your concrete methods final, they won't have
to wonder whether the generated subclass might be overriding them. This is
especially helpful if you are underriding equals, hashCode or toString!

Reference: https://github.com/google/auto/blob/master/value/userguide/practices.md#mark-all-concrete-methods-final

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AutoValueFinalMethods")` to the enclosing element.
