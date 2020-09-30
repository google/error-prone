---
title: AssertFalse
summary: Assertions may be disabled at runtime and do not guarantee that execution
  will halt here; consider throwing an exception instead
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Java assertions do not necessarily execute at runtime; they may be enabled and
disabled depending on which options are passed to the JVM invocation. An assert
false statement may be intended to ensure that the program never proceeds beyond
that statement. If the correct execution of the program depends on that being
the case, consider throwing an exception instead, so that execution is halted
regardless of runtime configuration.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AssertFalse")` to the enclosing element.
