---
title: CompileTimeConstant
layout: bugpattern
category: GUAVA
severity: ERROR
maturity: MATURE
---

# Bug pattern: CompileTimeConstant
__Non-compile-time constant expression passed to parameter with @CompileTimeConstant type annotation. If your expression is using another @CompileTimeConstant parameter, make sure that parameter is also marked final.__

## The problem
A method or constructor with one or more parameters whose declaration is annotated with the @CompileTimeConstant type annotation must only be invoked with corresponding actual parameters that are computed as compile-time constant expressions, such as a literal or static final constant.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CompileTimeConstant")` annotation to the enclosing element.
