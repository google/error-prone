---
title: BuilderReturnThis
summary: Builder instance method does not return 'this'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This check identifies instance methods in builder classes, and requires that
they either `return this;`, or are explicitly annotated with
`@CheckReturnValue`.

Instance methods in builders typically return `this`, to allow chaining.
Ignoring this result does not indicate a https://errorprone.info/bugpattern/CheckReturnValue bug. For
example, both of the following are fine:

```java
Foo.Builder builder = Foo.builder();
builder.setBar("bar"); // return value is deliberately unused
return builder.build();
```

```java
Foo.Builder builder =
    Foo.builder().setBar("bar").build();
```

Rarely, a builder method may return a new instance, which should not be ignored.
This check requires these methods to be annotated with `@CheckReturnValue`:

```java
class Builder {
  @CheckReturnValue
  Builder setFoo(String foo) {
    return new Builder(foo); // returns a new builder instead of this!
  }
}
```

This check allows the https://errorprone.info/bugpattern/CheckReturnValue enforcement to assume the
return value of instance methods in builders can safely be ignored, unless the
method is explicitly annotated with `@CheckReturnValue`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BuilderReturnThis")` to the enclosing element.
