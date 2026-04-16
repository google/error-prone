---
title: JUnitMethodInvoked
summary: Directly invoking a JUnit test method is discouraged; only the JUnit test
  runner should call these methods. If you need to share logic between tests, extract
  a helper method or class.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnitMethodInvoked")` to the enclosing element.
