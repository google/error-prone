---
title: NonRuntimeAnnotation
summary: Calling getAnnotation on an annotation that is not retained at runtime
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Calling getAnnotation on an annotation that does not have its Retention set to
RetentionPolicy.RUNTIME will always return null.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NonRuntimeAnnotation")` to the enclosing element.
