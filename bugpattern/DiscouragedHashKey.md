---
title: DiscouragedHashKey
summary: This type should not be used as an element of a hash-based Set, or a key
  of a hash-based Map, Multimap, or Multiset
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Hash-based containers call `equals` and `hashCode` on their elements and keys.
Some types are not suitable for this even though their `equals` and `hashCode`
methods are well-defined, for example because they are expensive to hash, or
because instances that represent the same logical value don't compare equal.

This check reports elements of hash-based `Set`s, and keys of hash-based `Map`s,
`Multimap`s, and `Multiset`s, whose types have been configured as unsuitable
hash keys. Prefer keying on a stable identifier for the value, or using a
non-hash-based container.

## Options

*   `-XepOpt:DiscouragedHashKey:Types=com.example.Foo,com.example.Bar`
    configures the types to report. Subtypes of the configured types are also
    reported.

Types whose `equals` methods are not meaningful, such as arrays or `Collection`,
are handled by [UndefinedEquals](UndefinedEquals.md) instead.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DiscouragedHashKey")` to the enclosing element.
