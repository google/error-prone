---
title: InjectOnMemberAndConstructor
summary: Members shouldn't be annotated with @Inject if constructor is already annotated
  @Inject
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When a class uses `@Inject` on a field, and that field is also assigned from an
`@Inject` constructor, then the field is assigned twice from the DI Injector.
This may result in 2 objects being created, where the first instance is assigned
then thrown away after the second injection.

A simple solution is to remove the `@Inject` annotation from the injected field.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InjectOnMemberAndConstructor")` to the enclosing element.
