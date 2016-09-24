---
title: MultibindsInsteadOfMultibindings
summary: '`@Multibinds` is the new way to declare multibindings.'
layout: bugpattern
category: DAGGER
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Nested `@Multibindings` interfaces are being replaced by `@Multibinds` methods in a module.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("MultibindsInsteadOfMultibindings")` annotation to the enclosing element.
