---
title: LoopToTestParameter
summary: Migrate loops in tests to use github.com/google/TestParameterInjector. Test
  parameterization executes each input case in strict isolation, ensuring that a single
  failure doesn't halt the rest of your test case while providing clear, per-case
  reporting without the need for manual loops.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LoopToTestParameter")` to the enclosing element.
