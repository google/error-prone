---
title: MixedDescriptors
summary: 'The field number passed into #getFieldByNumber belongs to a different proto
  to the Descriptor.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A proto's `Descriptor` was created by mixing the `Descriptors` class from one
proto with the field number from another. E.g.:

```java
Foo.getDescriptors().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```

This accesses the `Descriptor` of a field in `Foo` with a field number from
`Bar`. One of these was probably intended:

```java
Foo.getDescriptors().findFieldByNumber(Foo.ID_FIELD_NUMBER)
Bar.getDescriptors().findFieldByNumber(Bar.ID_FIELD_NUMBER)
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MixedDescriptors")` to the enclosing element.

