---
title: TheoryButNoTheories
summary: This test has members annotated with @Theory, @DataPoint, or @DataPoints but is using the default JUnit4 runner.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TheoryButNoTheories")` to the enclosing element.
