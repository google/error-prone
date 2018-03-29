---
title: InconsistentCapitalization
summary: It is confusing to have a field and a parameter under the same scope that differ only in capitalization.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
It is confusing to have two or more variables under the same scope that differ
only in capitalization. Make sure that both of these follow the casing guide
([Google Java Style Guide §5.3][styleCamelCase]) and to be consistent if more
than one option is possible.

This checker will only find parameters that differ in capitalization with fields
that can be accessed from the parameter's scope.

[styleCamelCase]: https://google.github.io/styleguide/javaguide.html#s5.3-camel-case

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InconsistentCapitalization")` to the enclosing element.
