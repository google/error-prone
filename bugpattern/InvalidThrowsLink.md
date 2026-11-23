---
title: InvalidThrowsLink
summary: Don't use {@link} or {@code} in @throws tags; mention the exception name
  directly (e.g., @throws IOException, not @throws {@link IOException}).
layout: bugpattern
tags: Style
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`@throws` does not require `@link`ing the target exception.

```java
/**
 * Validates {@code n}.
 *
 * @throws {@link Exception} if n is negative.
 */
void validate(int n) throws Exception {
  ...
}
```

```java
/**
 * Validates {@code n}.
 *
 * @throws Exception if n is negative.
 */
void validate(int n) throws Exception {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidThrowsLink")` to the element
being documented.

