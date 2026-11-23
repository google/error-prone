---
title: InvalidThrows
summary: The documented method doesn't actually throw this checked exception.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The `@throws` tag should not document a checked exception which is not actually
thrown by the documented method.

```java
/**
 * Validates {@code n}.
 *
 * @throws Exception if n is negative.
 */
void validate(int n) {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidThrows")` to the element being
documented.

