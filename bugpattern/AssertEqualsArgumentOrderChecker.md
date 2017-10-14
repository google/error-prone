---
title: AssertEqualsArgumentOrderChecker
summary: Arguments are swapped in assertEquals-like call
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
JUnit's assertEquals (and similar) are defined to take the expected value first and the actual value second. Getting these the wrong way round will cause a confusing error message if the assertion fails.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("AssertEqualsArgumentOrderChecker")` annotation to the enclosing element.
