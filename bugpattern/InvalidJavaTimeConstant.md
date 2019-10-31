---
title: InvalidJavaTimeConstant
summary: This checker errors on calls to java.time methods using values that are guaranteed to throw a DateTimeException.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("InvalidJavaTimeConstant")` to the enclosing element.
