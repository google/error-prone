---
title: CompileTimeConstant
summary: Non-compile-time constant expression passed to parameter with @CompileTimeConstant
  type annotation.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A method or constructor with one or more parameters whose declaration is
annotated with the `@CompileTimeConstant` type annotation must only be invoked
with corresponding actual parameters that are computed as compile-time constant
expressions, specifically expressions that:

*   the Java compiler can determine a constant value for at compile time (see
    [JLS §15.28](https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.28)),
    or
*   consist of the literal {@code null}, or
*   consist of a single identifier, where the identifier has the
    `@CompileTimeConstant` annotation and is one of the following:
    *   a class field that is declared {@code final}, or
    *   a formal parameter of a method or constructor, which is declared {@code
        final} or is effectively final, or
*   are formed from the concatenation of other compile-time constant `String`s,
    or
*   are switch expressions where all case bodies are compile-time constants, or
*   are ternary expressions with both branches being compile-time constants, or
*   are calls to one of the known immutable collection factory methods, where
    the provided elements are all compile-time constants.

For example, the following are valid compile-time constants:

*   `"some literal string"`
*   `"literal string" + compileTimeConstantParameter`
*   `debug ? compileTimeConstantParameter : "foo"`
*   `ImmutableList.of("a", "b", "c")`

When applied to fields, this check enforces that the field is `final` and has an
initializer which satisfies the above conditions.

Getting Java 8 references to methods with `@CompileTimeConstant` parameters is
disallowed because we couldn't check if the method reference is later applied to
a compile-time constant. Use the methods directly instead.

For the same reason, it's also disallowed to create lambda expressions with
`@CompileTimeConstant` parameters.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("CompileTimeConstant")` to the enclosing element.
