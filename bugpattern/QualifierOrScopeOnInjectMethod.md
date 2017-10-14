---
title: QualifierOrScopeOnInjectMethod
summary: Qualifiers/Scope annotations on @Inject methods don't have any effect. Move the qualifier annotation to the binding location.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("QualifierOrScopeOnInjectMethod")` annotation to the enclosing element.
