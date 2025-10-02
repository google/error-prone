---
title: InjectedConstructorAnnotations
summary: Injected constructors cannot be optional nor have binding annotations
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The constructor is annotated with @Inject(optional=true), or it is annotated
with @Inject and a binding annotation. This will cause a Guice runtime error.

For more information, see https://github.com/google/guice/wiki/InjectionPoints.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectedConstructorAnnotations")` to the enclosing element.
