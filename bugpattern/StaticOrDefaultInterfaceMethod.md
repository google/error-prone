---
title: StaticOrDefaultInterfaceMethod
summary: 'Static and default interface methods are not natively supported on older
  Android devices. '
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
Static and default interface methods are not natively supported on Android
versions earlier than 7.0. Enable this check for compatibility with older
devices. See [Android Java 8
Documentation](https://developer.android.com/guide/platform/j8-jack.html).


## Suppression

To declare default or static methods in interfaces, add a
`@SuppressWarnings("StaticOrDefaultInterfaceMethod")` annotation to the
enclosing element.

