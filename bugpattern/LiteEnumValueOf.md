---
title: LiteEnumValueOf
summary: Instead of converting enums to string and back, its numeric value should be used instead as it is the stable part of the protocol defined by the enum.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Byte code optimizers can change the implementation of `toString()` in lite
runtime and thus using `valueOf(String)` is discouraged. Instead of converting
enums to string and back, its numeric value should be used instead as it is the
stable part of the protocol defined by the enum.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("LiteEnumValueOf")` to the enclosing element.

