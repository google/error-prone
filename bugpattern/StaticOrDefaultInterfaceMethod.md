---
title: StaticOrDefaultInterfaceMethod
summary: Static and default methods in interfaces are not allowed in android builds.
layout: bugpattern
category: ANDROID
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("StaticOrDefaultInterfaceMethod")` annotation to the enclosing element.
