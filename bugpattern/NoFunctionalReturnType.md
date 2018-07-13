---
title: NoFunctionalReturnType
summary: Instead of returning a functional type, return the actual type that the returned function would return and use lambdas at use site.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Returning the actual type that the returned function would return instead of a functional type creates a more versatile method

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NoFunctionalReturnType")` to the enclosing element.
