---
title: JUnit4TestNotRun
summary: This looks like a test method but is not run; please add @Test and @Ignore,
  or, if this is a helper method, reduce its visibility.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Unlike in JUnit 3, JUnit 4 tests will not be run unless annotated with @Test.
The test method that triggered this error looks like it was meant to be a test,
but was not so annotated, so it will not be run. If you intend for this test
method not to run, please add both an @Test and an @Ignore annotation to make it
clear that you are purposely disabling it. If this is a helper method and not a
test, consider reducing its visibility to non-public, if possible.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnit4TestNotRun")` to the enclosing element.
