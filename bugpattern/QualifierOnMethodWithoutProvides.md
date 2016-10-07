---
title: QualifierOnMethodWithoutProvides
summary: Qualifier applied to a method that isn't a @Provides method. This method won't be used for dependency injection
layout: bugpattern
category: INJECT
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("QualifierOnMethodWithoutProvides")` annotation to the enclosing element.
