---
title: AssistedInjectAndInjectOnSameConstructor
summary: '@AssistedInject and @Inject cannot be used on the same constructor.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Using @AssistedInject and @Inject on the same constructor is a runtimeerror in
Guice.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AssistedInjectAndInjectOnSameConstructor")` to the enclosing element.
