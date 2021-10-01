---
title: Java8ApiChecker
summary: Use of class, field, or method that is not compatible with JDK 8
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Code that needs to be compatible with Java 8 cannot use types or members that are only present in newer class libraries

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("Java8ApiChecker")` to the enclosing element.
