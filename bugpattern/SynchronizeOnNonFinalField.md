---
title: SynchronizeOnNonFinalField
summary: 'Synchronizing on non-final fields is not safe: if the field is ever updated,
  different threads may end up locking on different objects.'
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Possible fixes:

*   If the field is never reassigned, add the missing `final` modifier.

*   If the field needs to be mutable, create a separate lock by adding a private
    final field and synchronizing on it to guard all accesses.

*   If the field is lazily initialized, annotation it with
    `com.google.errorprone.annotations.concurrent.LazyInit`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("SynchronizeOnNonFinalField")` annotation to the enclosing element.
