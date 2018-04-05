---
title: UseCorrectAssertInTests
summary: Java assert is used in test. For testing purposes Assert.* matchers should be used.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Java assert statements may or may not be evaluated depending on runtime flags to
the JVM invocation. When used in tests, this means that the test assertions may
not be checked, and a test may pass when it should actually fail. To avoid this,
use one of the assertion libraries that are always enabled, such as JUnit's
`org.junit.Assert` or Google's Truth library.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UseCorrectAssertInTests")` to the enclosing element.
