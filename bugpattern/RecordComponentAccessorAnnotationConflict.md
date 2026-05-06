---
title: RecordComponentAccessorAnnotationConflict
summary: Annotation on record component is ignored.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This check detects cases where an annotation on a record component is ignored.

## Method annotations

When an annotation that is applicable to methods is placed on a record
component, the Java compiler automatically propagates it to the generated
accessor method. If you provide an explicit accessor method instead of letting
the compiler generate one, the annotation is NOT automatically propagated to
your explicit accessor.

If you intended the annotation to be on the accessor method, you must repeat it
on the explicit accessor method declaration. If you did not intend it to be on
the accessor, and the annotation does not target `RECORD_COMPONENT`, then
placing it on the record component has no effect and is likely an error.

## Parameter annotations

When an annotation that is applicable to parameters is placed on a record
component, the Java compiler propagates it to the parameter of the generated
constructor. If you provide an explicit constructor, the annotation is not
automatically propagated to the parameters of the explicit constructor

If you intended the annotation to be on the parameter method, you must repeat it
on the parameter of the explicit constructor declaration. If you did not intend
it to be on the parameter, and the annotation does not target
`RECORD_COMPONENT`, then placing it on the record component has no effect and is
likely an error.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RecordComponentAccessorAnnotationConflict")` to the enclosing element.
