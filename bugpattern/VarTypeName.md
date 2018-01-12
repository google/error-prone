---
title: VarTypeName
summary: '`var` should not be used as a type name.'
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
As of JDK 10 `var` is a restricted local variable type and cannot be used for
type declarations (see [JEP 286][]).

[JEP 286]: http://openjdk.java.net/jeps/286

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("VarTypeName")` to the enclosing element.
