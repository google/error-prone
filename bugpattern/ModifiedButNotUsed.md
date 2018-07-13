---
title: ModifiedButNotUsed
summary: A collection or proto builder was created, but its values were never accessed.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Collections and proto builders which are created and mutated but never used may
be a sign of a bug, for example:

```java {.bad}
  MyProto.Builder builder = MyProto.newBuilder();
  if (field != null) {
    MyProto.NestedField.Builder nestedBuilder = MyProto.NestedField.newBuilder();
    nestedBuilder.setValue(field);
    // Oops--forgot to do anything with nestedBuilder.
  }
  return builder.build();
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ModifiedButNotUsed")` to the enclosing element.
