---
title: RedundantNullCheck
summary: Null check on an expression that is statically determined to be non-null
  according to language semantics or nullness annotations.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RedundantNullCheck")` to the enclosing element.
