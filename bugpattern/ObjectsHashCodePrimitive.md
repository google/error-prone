---
title: ObjectsHashCodePrimitive
summary: Objects.hashCode(Object o) should not be passed a primitive value
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`Objects.hashCode` takes an `Object` parameter, and will either return `0` when
the parameter is `null`, or call the underlying `hashCode` function of the
Object reference.

Passing a primitive value to `Objects.hashCode` function results in boxing the
primitive, then calling the boxed object's `hashCode`. You can get the same
result by using, e.g.: `Long.hashCode(long)` to get the effective hash code
of a primitive `long`. If you're calling this method outside of your own 
`hashCode()` implementation, prefer to use the `BoxedClass.hashCode(primitive)`
functions to avoid unwanted boxed.

If you're implementing a `hashCode` function for your **own** class that
consists of a single primitive value, you may want to consider some of these
alternatives:

```java
@Override
public int hashCode() {
  // This function will box intValue into an Integer, and wrap *that* in an
  // array, but will generate a hashCode which is likely to be different than
  // the hashCode of the boxed version of the intValue. This makes it easier
  // to add more fields to the class and hashCode method (just by adding more
  // fields to the hash call), but comes at a potential performance penalty.
  return Objects.hash(intValue);
}
```

```java
@Override
public int hashCode() {
  // This function will avoid boxing the int to an Integer, and is an explicit
  // acknowledgement that the hashCode() of *this* class is the same as the
  // hash code of the underlying intValue.
  return Integer.hashCode(intValue);
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ObjectsHashCodePrimitive")` to the enclosing element.
