---
title: ImmutableSetForContains
summary: This private static ImmutableList is only used for contains, containsAll
  or isEmpty checks; prefer ImmutableSet.
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
