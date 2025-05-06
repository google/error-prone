---
title: UnnecessaryQualifier
summary: A qualifier annotation has no effect here.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A `@Qualifier` or a `@BindingAnnotation` has no effect here, and can be removed.
Its presence may be misleading.

For example:

```java
final class MyInjectableClass {
  @Username private final String username;

  @Inject
  MyInjectableClass(@Username String username) {
    this.username = username;
  }
}
```

The annotation on the constructor parameter is important, but the field
annotation is redundant.

```java
final class MyInjectableClass {
  private final String username;

  @Inject
  MyInjectableClass(@Username String username) {
    this.username = username;
  }
}
```

There are a couple of ways this check can lead to false positives:

*   You're using a custom framework we don't know about which makes the location
    of the finding an injection point. File a bug, and we'll happily incorporate
    it.

*   Your annotation is annotated with `@Qualifier` or `@BindingAnnotation` but
    isn't actually used as a qualifier (perhaps you have a framework that just
    uses it reflectively). Try removing those annotations from the *annotation*.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryQualifier")` to the enclosing element.
