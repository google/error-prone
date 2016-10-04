---
title: ClassName
summary: The source file name should match the name of the top-level class it contains
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Google Java Style Guide §2.1 states, "The source file name consists of the
case-sensitive name of the top-level class it contains, plus the .java
extension."

## Suppression

Since `@SuppressWarnings` cannot be applied to package declarations, this
warning can be suppressed by annotating any top-level class in the compilation
unit with `@SuppressWarnings("ClassName")`.

