---
title: TypeEquals
summary: com.sun.tools.javac.code.Type doesn't override Object.equals and instances are not interned by javac, so testing types for equality should be done with Types#isSameType instead
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


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TypeEquals")` to the enclosing element.
