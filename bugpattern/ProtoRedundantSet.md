---
title: ProtoRedundantSet
summary: A field on a protocol buffer was set twice in the same chained expression.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Proto builders provide a pleasant fluent interface for constructing instances.
Unlike argument lists, however, they do not prevent the user from providing
multiple values for the same field.

Setting the same field multiple times in the same chained expression is
pointless (as the intermediate value will be overwritten), and certainly
unintentional. If the field is set to different values, it may be a bug, e.g.,

```java
return MyProto.newBuilder()
    .setFoo(copy.getFoo())
    .setFoo(copy.getBar())
    .build();
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtoRedundantSet")` to the enclosing element.
