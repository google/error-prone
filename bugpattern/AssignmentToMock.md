---
title: AssignmentToMock
summary: Fields annotated with @Mock should not be manually assigned to.
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
The `@Mock` annotation is used to automatically initialize mocks using
`MockitoAnnotations.initMocks`, or `MockitoJUnitRunner`.

Variables annotated this way should not be explicitly initialized, as this will
be overwritten by automatic initialization.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AssignmentToMock")` to the enclosing element.
