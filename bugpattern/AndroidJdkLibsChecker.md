---
title: AndroidJdkLibsChecker
summary: Use of class, field, or method that is not compatible with legacy Android devices
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: Java7ApiChecker, AndroidApiChecker_

## The problem
Code that needs to be compatible with Android cannot use types or members that only the latest or unreleased devices can handle

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AndroidJdkLibsChecker")` to the enclosing element.
