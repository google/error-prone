---
title: DoNotCall
summary: This method should not be called.
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
This check prevents calls to methods annotated with Error Prone's `@DoNotCall`
annotation (`com.google.errorprone.annotations.DoNotCall`).

The check disallows invocations and method references of the annotated method.

There are a few situations where this can be useful:

*   methods that are required to satisfy the contract of an interface, but that
    are not supported

*   the method works, but its implementation should always be inlined into your
    own code

A method annotated with `@DoNotCall` should always be `final` or `abstract`. If
an `abstract` method is annotated `@DoNotCall` Error Prone will ensure all
implementations of that method also have the annotation. Methods annotated with
`@DoNotCall` should *not* be private, since a private method that should not be
called can simply be removed.

TIP: Marking methods annotated with `@DoNotCall` as `@Deprecated` is
recommended, since it provides IDE users with more immediate feedback.

Example:

`java.util.Collection#add` should never be called on an immutable collection
implementation:

```java
package com.google.common.collect.ImmutableList;

class ImmutableList<E> implements List<E> {

 // ...

 /**
  * Guaranteed to throw an exception and leave the list unmodified.
  *
  * @deprecated Unsupported operation.
  */
 @Deprecated
 @DoNotCall("guaranteed to throw an exception and leave the list unmodified")
 @Override
 public final void add(E e) {
   throw new UnsupportedOperationException();
 }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DoNotCall")` to the enclosing element.
