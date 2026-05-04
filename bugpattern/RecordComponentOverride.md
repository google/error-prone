---
title: RecordComponentOverride
summary: '@Override annotations on record components don''t do anything.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Because `@Override` is a `@Target(METHOD)` annotation, it's automatically
permitted on record component declarations. It would normally cause the
annotation to be copied to the generated accessor method, but in this case it's
a SOURCE-retention annotation so there's nothing to do, and the annotation is
ignored.

Note that the annotation *does not* mean that the generated accessor method for
the record is overriding something.

Also note that a hand-written accessor method in a record class can also always
use `@Override` regardless of supertype methods (see [JLS §9.6.4.4]):

> If a method declaration in class or interface Q is annotated with `@Override`,
> then one of the following three conditions must be true, or a compile-time
> error occurs:
>
> ...
>
> Q is a record class (§8.10), and the method is an accessor method for a record
> component of Q (§8.10.3)

[JLS §9.6.4.4]: https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.6.4.4

For additional discussion, see
[this compile-dev@ thread](https://mail.openjdk.org/pipermail/compiler-dev/2021-July/017602.html)
and
[Error Prone issue #5174](https://github.com/google/error-prone/issues/5174).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RecordComponentOverride")` to the enclosing element.
