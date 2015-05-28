---
title: ProtoStringFieldReferenceEquality
summary: "Comparing protobuf fields of type String using reference equality"
layout: bugpattern
category: ONE_OFF
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Comparing strings with == is almost always an error, but it is an error 100% of the time when one of the strings is a protobuf field.  Additionally, protobuf fields cannot be null, so Object.equals(Object) is always more correct.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ProtoStringFieldReferenceEquality")` annotation to the enclosing element.
