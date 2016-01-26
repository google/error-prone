---
title: Var
summary: Non-constant variable missing @Var annotation
layout: bugpattern
category: JDK
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Most references are never modified, and accidentally modifying a reference is a
potential source of bugs.

One option for avoiding accidental modification is to annotate all variables
that are not modified as `final`. However that approach is very noisy, since
most variables are never modified, and accidental modification is only avoided
if you remember to add `final` everywhere.

A better solution is to invert the default, and assume that all variables are
constant unless they are explicitly declared as modifiable. The `@Var`
annotation provides a way to mark variables as modifiable. The accompanying
Error Prone check enforces that all modifiable fields, parameters, and local
variables are explicitly annotated with `@Var`.

Since Java 8 can infer whether a local variable or parameter is effectively
`final`, and `@Var` makes it clear whether any variable is non-`final`,
explicitly marking local variables and parameters as `final` is discouraged. All
fields declarations should be explicitly marked either `final` or `@Var`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("Var")` annotation to the enclosing element.
