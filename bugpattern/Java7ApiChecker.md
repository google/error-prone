---
title: Java7ApiChecker
summary: Use of class, field, or method that is not compatible with JDK 7
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
Code that needs to be compatible with Java 7 cannot use types or members that are only present in the JDK 8 class libraries

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("Java7ApiChecker")` to the enclosing element.
