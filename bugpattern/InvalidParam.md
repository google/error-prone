---
title: InvalidParam
summary: This @param tag doesn't refer to a parameter of the method.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Javadoc's `@param` tag should not be used to document parameters which do not
appear in the formal parameter list. This may indicate a typo, or an omission
when refactoring.

```java
/**
 * Parses {@code input} as a proto.
 *
 * @param inputBytes input bytes to parse.
 */
MyProto parse(byte[] input) {
  ...
}
```

## Suppression

Suppress by applying `@SuppressWarnings("InvalidParam")` to the element being
documented.

