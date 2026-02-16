---
title: AddNullMarkedToPackageInfo
summary: Apply @NullMarked to this package
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Checks if `@NullMarked` annotation is applied to a `package-info.java` file.

More details are available at [AddNullMarkedToClass](AddNullMarkedToClass.md)

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AddNullMarkedToPackageInfo")` to the enclosing element.
