---
title: IsInstanceOfClass
summary: The argument to Class#isInstance(Object) should not be a Class
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Passing an argument of type `Class` to `Class#instanceOf(Class)` is usually a
mistake.

Calling `clazz.instanceOf(obj)` for some `clazz` with type `Class<T>` is
equivalent to `obj instanceof T`. The `instanceOf` method exists for cases
where the type `T` is not known statically.

When a class literal is passed as the argument of `instanceOf`, the result will
only be true if the class literal on left hand side is equal to `Class.class`.

For example, the following code returns true if and only if the type `A` is
equal to `Class` (i.e. lhs is equal to `Class.class`).

```java
<A, B> boolean f(Class<A> lhs, Class<B> rhs) {
  return lhs.instanceOf(rhs); // equivalent to 'lhs == Class.class'
}
```

To test if the type represented by a class literal is a subtype of the type
reprsented by some other class literal, `isAssignableFrom` should be used
instead:

```java
<A, B> boolean f(Class<A> lhs, Class<B> rhs) {
  return lhs.isAssignableFrom(rhs); // equivalent to 'B instanceof A'
}
```

## Suppression
Suppress false positives by adding an `@SuppressWarnings("IsInstanceOfClass")` annotation to the enclosing element.
