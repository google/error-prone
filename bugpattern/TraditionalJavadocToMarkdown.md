---
title: TraditionalJavadocToMarkdown
summary: Converts traditional Javadoc comments into Markdown Javadoc comments
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Converts traditional Javadoc comments into Markdown Javadoc comments.

```java
/**
 * A method with Javadoc.
 *
 * @param x description
 */
public void foo(int x) {}
```

```java
/// A method with Javadoc.
///
/// @param x description
public void foo(int x) {}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TraditionalJavadocToMarkdown")` to the enclosing element.
