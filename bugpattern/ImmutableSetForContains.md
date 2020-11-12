---
title: ImmutableSetForContains
summary: ImmutableSet is a more efficient type for private static final constants
  if the constant is only used for contains, containsAll or isEmpty checks.
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ImmutableSetForContains")` to the enclosing element.
