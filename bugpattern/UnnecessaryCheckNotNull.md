---
title: UnnecessaryCheckNotNull
summary: By specification, a constructor cannot return a null value, so invoking Preconditions.checkNotNull(...) or Verify.verifyNotNull(...) is redundant
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryCheckNotNull")` to the enclosing element.
