---
title: ExtendsAutoValue
summary: Do not extend an @AutoValue/@AutoOneOf class in non-generated code.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`@AutoValue` classes are intended to be closed, with a single implementation
with known semantics. Implementing them by hand is extremely dangerous.

Here are some common cases where we have seen code that extends `@AutoValue`
classes and recommendations for what to do instead:

*   **Overriding the getters to return given values.** This should usually be
    replaced by creating an instance of the `@AutoValue` class with those
    values.

*   **Having the `@AutoValue.Builder` class extend the `@AutoValue` class so it
    inherits the abstract getters.** This is wrong since the Builder doesn't
    satisfy the contract of its superclass and is better implemented either by
    repeating the methods or by having both the `@AutoValue` class and the
    Builder implement a common interface with these getters.

*   In other cases of extending the `@AutoValue` class and implementing its
    abstract methods, it would be more correct to have the `@AutoValue` class
    and the other class have a common supertype.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ExtendsAutoValue")` to the enclosing element.
