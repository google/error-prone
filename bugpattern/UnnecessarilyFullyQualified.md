---
title: UnnecessarilyFullyQualified
summary: This fully qualified name is unambiguous to the compiler if imported.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Prefer using imported names to refer to classes, unless a qualified name is
necessary to disambiguate two classes with the same name.

That is, prefer this:

```java
import java.util.ArrayList;
import java.util.List;

class Test {
  List<String> names = new ArrayList<>();
}
```

instead of this:

```java
class Test {
  java.util.List<String> names = new java.util.ArrayList<>();
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessarilyFullyQualified")` to the enclosing element.
