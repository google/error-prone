---
title: CompileTimeConstant
summary: Non-compile-time constant expression passed to parameter with @CompileTimeConstant type annotation.
layout: bugpattern
category: GUAVA
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
A method or constructor with one or more parameters whose declaration is annotated with the @CompileTimeConstant type annotation must only be invoked with corresponding actual parameters that are computed as compile-time constant expressions, such as a literal or static final constant.

## Suppression
This check may not be suppressed.
