---
title: ProvidesMethodOutsideOfModule
summary: '@Provides methods need to be declared in a Module to have any effect.'
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Guice `@Provides` methods annotate methods that are used as a means of declaring
bindings. However, this is only helpful inside of a module. Methods outside of
these modules are not used for binding declaration.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProvidesMethodOutsideOfModule")` to the enclosing element.
