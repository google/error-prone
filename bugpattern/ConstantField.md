---
title: ConstantField
summary: Field name is CONSTANT_CASE, but field is not static and final
layout: bugpattern
tags: ''
severity: SUGGESTION
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The [Google Java Style Guide §5.2.4][style] requires constant names to use
`CONSTANT_CASE`.

[style]: https://google.github.io/styleguide/javaguide.html#s5.2.4-constant-names

When naming a field with `CONSTANT_CASE`, make sure the field is `static`,
`final`, and of immutable type. If the field doesn't meet those criteria, use
`lowerCamelCase` instead.

The check recognizes all primitive, `String`, and `enum` fields as deeply
immutable. It is possible to create mutable enums, but doing so is
strongly discouraged.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ConstantField")` annotation to the enclosing element.
