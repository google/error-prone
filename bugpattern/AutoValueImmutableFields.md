---
title: AutoValueImmutableFields
summary: AutoValue recommends using immutable collections
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: mutable_

## The problem
[AutoValue](https://github.com/google/auto/tree/master/value) instances should
be deeply immutable. Therefore, we recommend using immutable types for fields.
E.g., use `ImmutableMap` instead of `Map`, `ImmutableSet` instead of `Set`, etc.

Read more at:

*   https://github.com/google/auto/blob/master/value/userguide/practices.md#avoid-mutable-property-types
*   https://github.com/google/auto/blob/master/value/userguide/builders-howto.md#-use-a-collection-valued-property

Suppress violations by using `@SuppressWarnings("AutoValueImmutableFields")` on
the relevant `abstract` getter.

