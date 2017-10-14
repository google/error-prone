---
title: JMockTestWithoutRunWithOrRuleAnnotation
summary: jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field must have a @Rule JUnit annotation
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
jMock tests must have a @RunWith(JMock.class) annotation, or the Mockery field must have a @Rule JUnit annotation. If this is not done, then all of your jMock tests will run and pass, but none of your assertions will actually be evaluated. Your tests will pass even if they shouldn't.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("JMockTestWithoutRunWithOrRuleAnnotation")` annotation to the enclosing element.
