---
title: UnnecessaryTestMethodPrefix
summary: A `test` prefix for a JUnit4 test is redundant, and a holdover from JUnit3.
  The `@Test` annotation makes it clear it's a test.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefixing JUnit4 test methods with `test` is unnecessary and redundant given the
use of the `@Test` annotation makes it clear that they're tests.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryTestMethodPrefix")` to the enclosing element.
