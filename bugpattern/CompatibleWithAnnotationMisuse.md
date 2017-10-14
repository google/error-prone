---
title: CompatibleWithAnnotationMisuse
summary: '@CompatibleWith''s value is not a type argument.'
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
The `@CompatibleWith` annotation is used to mark parameters that need extra type checking on arguments passed to the method. The annotation was not appropriately placed on a parameter with a valid type argument. See the javadoc for more details.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CompatibleWithAnnotationMisuse")` annotation to the enclosing element.
