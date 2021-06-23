---
title: JUnit4TestsNotRunWithinEnclosed
summary: This test is annotated @Test, but given it's within a class using the Enclosed
  runner, will not run.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnit4TestsNotRunWithinEnclosed")` to the enclosing element.
