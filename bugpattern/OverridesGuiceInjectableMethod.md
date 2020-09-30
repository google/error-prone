---
title: OverridesGuiceInjectableMethod
summary: This method is not annotated with @Inject, but it overrides a method that
  is annotated with @com.google.inject.Inject. Guice will inject this method, and
  it is recommended to annotate it explicitly.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Unlike with `@javax.inject.Inject`, if a method overrides a method annotated
with `@com.google.inject.Inject`, Guice will inject it even if it itself is not
annotated. This differs from the behavior of methods that override
`@javax.inject.Inject` methods since according to the JSR-330 spec, a method
that overrides a method annotated with `@javax.inject.Inject` will not be
injected unless it iself is annotated with `@Inject`. Because of this
difference, it is recommended that you annotate this method explicitly.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OverridesGuiceInjectableMethod")` to the enclosing element.
