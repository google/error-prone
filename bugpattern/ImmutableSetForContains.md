---
title: ImmutableSetForContains
summary: This private static ImmutableList either does not contain duplicates or is
  only used for contains, containsAll or isEmpty checks or both. ImmutableSet is a
  better type for such collection. It is often more efficient and / or captures useful
  info about absence of duplicates.
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
