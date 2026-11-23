---
title: InheritDoc
summary: Invalid use of @inheritDoc.
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The `@inheritDoc` tag should only be used on classes/interfaces which extend or
implement another and methods which override a method from a superclass.

```java
class Frobnicator {
  /** {@inheritDoc} */
  public void frobnicate() {
    // ...
  }
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InheritDoc")` to the element being
documented.

