---
title: GuiceAssistedInjectScoping
summary: Scope annotation on implementation class of AssistedInject factory is not allowed
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Classes that AssistedInject factories create may not be annotated with scope
annotations, such as @Singleton. This will cause a Guice error at runtime.

See [https://code.google.com/p/google-guice/issues/detail?id=742 this bug
report] for details.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("GuiceAssistedInjectScoping")` to the enclosing element.
