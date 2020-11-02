---
title: BadAnnotationImplementation
summary: Classes that implement Annotation must override equals and hashCode. Consider
  using AutoAnnotation instead of implementing Annotation by hand.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Implementations of [`Annotation`] must override `equals` and `hashCode` to match
the expectations defined in that interface, to ensure compatibility with the
annotation instances that source annotations create. Without this, operations
that care about equality of annotations (such as qualified dependency injection
bindings) will fail in mysterious ways.

```java
class Foo {
  @SomeAnnotation("hello") public void annotatedMethod() {}

  private static class HelloAnnotationImpl implements SomeAnnotation {
    @Override
    public Class<? extends Annotation> annotationType() {
      return SomeAnnotation.class;
    }

    @Override
    public String value() {
      return "hello";
    }
  }

  static void test() {
    Annotation manual = new HelloAnnotationImpl();
    Annotation fromMethod = Foo.class.getMethod("annotatedMethod").getDeclaredAnnotations()[0];

    manual.equals(fromMethod); // false, violating equality expectations of Annotation!
  }
}
```

It is very difficult to write these methods correctly, so consider using
[`@AutoAnnotation`] to generate a properly-functioning implementation of
`Annotation`:

```java
class Foo {
  @SomeAnnotation("hello") public void annotatedMethod() {}

  @AutoAnnotation
  private static SomeAnnotation someAnnotationInstance(String value) {
    return new AutoAnnotation_Foo_someAnnotationInstance(value);
  }

  static void test() {
    Annotation manual = someAnnotationInstance("hello");
    Annotation fromMethod = Foo.class.getMethod("annotatedMethod").getDeclaredAnnotations()[0];

    manual.equals(fromMethod); // true, hooray!
  }
}
```

[`Annotation`]: https://docs.oracle.com/javase/8/docs/api/java/lang/annotation/Annotation.html
[`@AutoAnnotation`]: https://github.com/google/auto/blob/master/value/src/main/java/com/google/auto/value/AutoAnnotation.java

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BadAnnotationImplementation")` to the enclosing element.
