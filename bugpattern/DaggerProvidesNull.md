---
title: DaggerProvidesNull
summary: Dagger @Provides methods may not return null unless annotated with @Nullable
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Dagger `@Provides` methods may not return null unless annotated with `@Nullable`. Such a method will cause a `NullPointerException` at runtime if the `return null` path is ever taken.

If you believe the `return null` path can never be taken, please throw a `RuntimeException` instead. Otherwise, please annotate the method with `@Nullable`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("DaggerProvidesNull")` annotation to the enclosing element.
