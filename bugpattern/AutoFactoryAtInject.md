---
title: AutoFactoryAtInject
summary: '@AutoFactory and @Inject should not be used in the same type.'
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
@AutoFactory classes should not be @Inject-ed, inject the generated factory
instead. Classes that are annotated with @AutoFactory are intended to be
constructed by invoking the factory method on the generated factory. Typically
this is because some of the necessary constructor arguments are not part of the
binding graph. Generated @AutoFactory classes are automatically marked @Inject -
prefer to inject that instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AutoFactoryAtInject")` to the enclosing element.
