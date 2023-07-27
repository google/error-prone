---
title: OverridingMethodInconsistentArgumentNamesChecker
summary: Arguments of overriding method are inconsistent with overridden method.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Inconsistently ordered parameters in method overrides mostly indicate an
accidental bug in the overriding method. An example for an overriding method
with inconsistent parameter names:

```java
class A {
  public void foo(int foo, int baz) { ... }
}

class B extends A {
  @Override
  public void foo(int baz, int foo) { ... }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OverridingMethodInconsistentArgumentNamesChecker")` to the enclosing element.
