---
title: BootclasspathShadowing
summary: Top-level class shadows a class on the compilation bootclasspath or platform
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: PlatformClassShadowing_

## The problem
Declaring a top-level class whose binary name already exists on the compilation
bootclasspath or platform modules (`StandardLocation.PLATFORM_CLASS_PATH` or
`StandardLocation.SYSTEM_MODULES`) shadows the platform class during compilation
or at runtime depending on classloader delegation order, which can cause
unexpected linkage errors, duplicate class conflicts, or subtle behavioral
mismatches.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BootclasspathShadowing")` to the enclosing element.
