---
title: RedundantSetterCall
summary: A field was set twice in the same chained expression.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ProtoRedundantSet_

## The problem
Proto and AutoValue builders provide a fluent interface for constructing
instances. Unlike argument lists, however, they do not prevent the user from
providing multiple values for the same field.

Setting the same field multiple times in the same chained expression is
pointless (as the intermediate value will be overwritten), and can easily mask a
bug, especially if the setter is called with *different* arguments.

```java
return MyProto.newBuilder()
    .setFoo(copy.getFoo())
    .setFoo(copy.getBar())
    .build();
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RedundantSetterCall")` to the enclosing element.
