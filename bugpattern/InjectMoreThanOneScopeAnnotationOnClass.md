---
title: InjectMoreThanOneScopeAnnotationOnClass
summary: A class can be annotated with at most one scope annotation.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: MoreThanOneScopeAnnotationOnClass_

## The problem
Annotating a class with more than one scope annotation is invalid according to
the JSR-330 specification.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectMoreThanOneScopeAnnotationOnClass")` to the enclosing element.
