---
title: NullablePrimitiveArray
summary: '@Nullable type annotations should not be used for primitive types since
  they cannot be null'
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
For `@Nullable` type annotations (such as
`org.checkerframework.checker.nullness.qual.Nullable`), `@Nullable byte[]` means
a 'non-null array of nullable bytes', and `byte @Nullable []` means a 'nullable
array of non-null bytes'. Since primitive types cannot be null, the former is
incorrect.

Some other nullness annotations (such as `javax.annotation.Nullable`) are
_declaration_ annotations rather than _type_ annotations. Their meaning is
different: For such annotations, `@Nullable byte[]` refers to 'a nullable array
of non-null bytes,' and `byte @Nullable []` is rejected by javac. Thus, this
check never reports errors for usages of declaration annotations.

See also: https://checkerframework.org/manual/#faq-array-syntax-meaning

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullablePrimitiveArray")` to the enclosing element.
