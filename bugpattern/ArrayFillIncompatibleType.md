---
title: ArrayFillIncompatibleType
summary: Arrays.fill(Object[], Object) called with incompatible types.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`Arrays.fill(Object[], Object)` is used to copy a reference into every slot of
an array.

For example:

```java
String[] foo = new String[42];
Arrays.fill(foo, "life");
// 42 references to the same String instance of "life" in the foo array
```

However, because of Array covariance (e.g.: `String[]` is assignable to
`Object[]`), and the signature of Arrays.fill is `Arrays.fill(Object[],
Object)`, this also allows you to do the following:

```java
String[] foo = new String[42];
Arrays.fill(foo, 42); // ArrayStoreException! Integer can't be put into a String[]
```

This check detects the above circumstances, and won't let you attempt to put
`Integer`s into a `String[]`.

## What about Lists?

`List<T>` doesn't have the same issue, since generic types are _not_ covariant.

```java
List<String> foo = new ArrayList<>();
foo.add(42); // Compile time error: Integer is not assignable to String
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArrayFillIncompatibleType")` annotation to the enclosing element.
