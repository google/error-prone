---
title: ProtoFieldNullComparison
summary: Protobuf fields cannot be null.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
This checker looks for comparisons of protocol buffer fields with null. If a
proto field is not specified, its field accessor will return a non-null default
value. Thus, the result of calling one of these accessors can never be null, and
comparisons like these often indicate a nearby error.

If you need to distinguish between an unset optional value and a default value,
you have two options. In most cases, you can simply use the `hasField()` method.
proto3 however does not generate `hasField()` methods for scalar fields of type
`string` or `bytes`. In those cases you will need to wrap your field in
`google.protobuf.StringValue` or `google.protobuf.BytesValue`, respectively.

```java {.bad}
void test(MyProto proto) {
  if (proto.getField() == null) {
    ...
  }
  if (proto.getRepeatedFieldList() != null) {
    ...
  }
}
```

```java {.good}
void test(MyProto proto) {
  if (!proto.hasField()) {
    ...
  }
  if (!proto.getRepeatedFieldList().isEmpty()) {
    ...
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtoFieldNullComparison")` to the enclosing element.
