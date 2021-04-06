---
title: TestParametersNotInitialized
summary: This test has @TestParameter fields but is using the default JUnit4 runner.
  The parameters will not be initialised beyond their default value.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TestParametersNotInitialized")` to the enclosing element.
