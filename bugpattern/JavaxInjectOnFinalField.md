---
title: JavaxInjectOnFinalField
summary: '@javax.inject.Inject cannot be put on a final field.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
According to the JSR-330 spec, the @javax.inject.Inject annotation cannot go on
final fields.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaxInjectOnFinalField")` to the enclosing element.
