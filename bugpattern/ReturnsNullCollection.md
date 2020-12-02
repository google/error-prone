---
title: ReturnsNullCollection
summary: Methods has a collection return type and returns {@code null} in some cases
  but does not annotate the method as @Nullable. See Effective Java 3rd Edition Item
  54.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ReturnsNullCollection")` to the enclosing element.
