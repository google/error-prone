---
title: MixedDescriptors
summary: 'The field number passed into #findFieldByNumber belongs to a different proto
  to the Descriptor.'
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A field `Descriptor` was created by mixing the message `Descriptor` from one
proto message with the field number from another. For example:

```java
Foo.getDescriptor().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```

This accesses the `Descriptor` of a field in `Foo` with a field number from
`Bar`. One of these was probably intended:

```java
Foo.getDescriptor().findFieldByNumber(Foo.ID_FIELD_NUMBER)
Bar.getDescriptor().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MixedDescriptors")` to the enclosing element.
