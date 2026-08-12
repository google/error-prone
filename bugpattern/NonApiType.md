---
title: NonApiType
summary: Certain types should not be passed across API boundaries.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Flags instances of non-API types from being accepted or returned in public APIs.

### What it flags

*   **Primitive Arrays:** Methods accepting or returning primitive or
    boxed-primitive arrays (`int[]`, `Integer[]`, `double[]`, `Double[]`,
    `long[]`, `Long[]`). Prefer `ImmutableIntArray`, `ImmutableDoubleArray`, or
    `ImmutableLongArray` instead.
    *   *Note:* Var-args parameters (e.g. `int... rest`) are **not** flagged, as
        var-args is idiomatic Java for parameter lists.
*   **Collection Implementations:** Accepting or returning concrete collection
    classes (`ArrayList`, `LinkedList`, `HashSet`, `LinkedHashSet`, `TreeSet`,
    `HashMap`, `LinkedHashMap`, `TreeMap`). Prefer interface types (`List`,
    `Set`, `Map`).
*   **Immutable Collections as Parameters:** Accepting `ImmutableCollection`,
    `ImmutableList`, `ImmutableSet`, or `ImmutableMap` as method parameters.
    Prefer accepting `Collection`, `List`, `Set`, `Map`, or `Iterable` for
    parameter generality.
*   **Optional Parameters:** Accepting `java.util.Optional` or
    `com.google.common.base.Optional` as method parameters. Prefer method
    overloading: creating one signature with the parameter and one without (or
    use `@Nullable` parameters).
*   **`com.google.common.base.Pair`:** Passing `Pair` across API boundaries.
    Define a well-named class or record instead.
*   **Iterators & Streams:** Returning `Iterator` (prefer `Stream` or collecting
    to an `ImmutableList`/`ImmutableSet`) or accepting `Stream` as a parameter
    (prefer `Iterable` or `Collection`).
    *   Returning stateful single-use `Iterator`s limits caller options
*   **ProtoTime Types:** Using `com.google.protobuf.Duration`, `Timestamp`, or
    `com.google.type.*` types across public APIs instead of standard
    `java.time.*` types (`Duration`, `Instant`, `LocalDate`, etc.).
*   **Flogger Loggers:** Passing `FluentLogger` or `GoogleLogger` instances
    across method boundaries; this can break standard per-class logger
    initialization patterns.

### Why

*   **Type Generality & Interface Abstraction:** Methods should accept abstract
    interface types (e.g., `List` rather than `ArrayList`) to give callers
    flexibility in implementation details.
*   **Immutability & Safety:** Primitive arrays are mutable and expose internal
    array state directly to callers. Guava's `ImmutableIntArray`,
    `ImmutableDoubleArray`, and `ImmutableLongArray` provide immutable, safe,
    and efficient alternatives.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NonApiType")` to the enclosing element.
