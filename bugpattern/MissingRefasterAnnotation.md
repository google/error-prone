---
title: MissingRefasterAnnotation
summary: The Refaster template contains a method without any Refaster annotations
layout: bugpattern
tags: LikelyError
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
A Refaster template consists of multiple methods. Typically, each method in the
class has an annotation. If a method has no annotation, this is likely an
oversight.

```java
static final class MethodLacksBeforeTemplateAnnotation {
  @BeforeTemplate
  boolean before1(String string) {
    return string.equals("");
  }

  // @BeforeTemplate is missing
  boolean before2(String string) {
    return string.length() == 0;
  }

  @AfterTemplate
  @AlsoNegation
  boolean after(String string) {
    return string.isEmpty();
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MissingRefasterAnnotation")` to the enclosing element.
