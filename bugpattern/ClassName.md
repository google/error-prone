---
title: ClassName
summary: The source file name should match the name of the top-level class it contains
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Google Java Style Guide § 2.1 states, "The source file name consists of the case-sensitive name of the top-level class it contains, plus the .java extension."

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ClassName")` annotation to the enclosing element.
