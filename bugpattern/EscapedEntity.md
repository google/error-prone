---
title: EscapedEntity
summary: HTML entities in @code/@literal tags will appear literally in the rendered
  javadoc.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
HTML entities used within `@code` and `@literal` tags will be interpreted
directly rather than converted to the expected characters. For example, this is
wrong:

```java
/**
 * <pre>{@code
 *   &#064;Override
 *   public boolean equals(Object o) {
 *     return false;
 *   }
 * }</pre>
 */
```

An option is to drop the {@code } tags, though this will then require escaping
any generic type parameters which may otherwise be interpreted as HTML. That is,
`List<Integer>` is the text "List" followed by the (non-existent) tag "Integer".

```java
/**
 * <pre>
 *   &#064;Override
 *   public boolean equals(Object o) {
 *     return false;
 *   }
 * </pre>
 */
```

## Suppression

Suppress by applying `@SuppressWarnings("EscapedEntity")` to the element being
documented.

