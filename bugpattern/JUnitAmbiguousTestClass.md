---
title: JUnitAmbiguousTestClass
summary: Test class inherits from JUnit 3's TestCase but has JUnit 4 @Test annotations.
layout: bugpattern
category: JUNIT
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
JUnit 3 uses inheritance, whereas JUnit 4 uses composition. Mixing these two design patterns historically has been a source of test bugs and unexpected behavior (e.g.: teardown logic and/or verification does not run). Error Prone also cannot infer whether the test class runs with JUnit 3 or JUnit 4. Thus, even if the test class runs with JUnit 4, Error Prone will not run additional checks which can catch common errors with JUnit 4 test classes. Either use only JUnit 4 and remove the inheritance from TestCase, or use only JUnit 3 and remove the @Test annotations.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JUnitAmbiguousTestClass")` annotation to the enclosing element.
