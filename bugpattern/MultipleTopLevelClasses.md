---
title: MultipleTopLevelClasses
summary: Source files should not contain multiple top-level class declarations
layout: bugpattern
category: JDK
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: TopLevel_

## The problem
The Google Java Style Guide §3.4.1 requires each source file to contain exactly
one top-level class.

## Suppression

Since `@SuppressWarnings` cannot be applied to package declarations, this
warning can be suppressed by annotating any top-level class in the compilation
unit with `@SuppressWarnings("MultipleTopLevelClasses")`.

