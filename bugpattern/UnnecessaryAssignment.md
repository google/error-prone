---
title: UnnecessaryAssignment
summary: Fields annotated with @Inject/@Mock/@TestParameter should not be manually
  assigned to, as they should be initialized by a framework. Remove the assignment
  if a framework is being used, or the annotation if one isn't.
layout: bugpattern
tags: ''
severity: WARNING
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryAssignment")` to the enclosing element.
