---
title: NonCanonicalType
summary: This type is referred to by a non-canonical name, which may be misleading.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Types being referred to by non-canonical names can be confusing. For example,

```java
public final class Entries {
  private final ImmutableList<ImmutableMap.Entry<String, Long>> entries;

  public Entries(Map<String, Long> map) {
    this.entries = ImmutableList.copyOf(map.entrySet());
  }
}
```

There is nothing special about `ImmutableMap.Entry`; it is precisely the same
type as `Map.Entry`. This example makes it look deceptively as though
`ImmutableList<ImmutableMap.Entry<?, ?>>` is an immutable type and therefore
safe to store indefinitely, when really it offers no more safety than
`ImmutableList<Map.Entry<?, ?>>`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NonCanonicalType")` to the enclosing element.

