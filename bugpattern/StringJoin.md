---
title: StringJoin
summary: String.join(CharSequence) performs no joining (it always returns the empty
  string); String.join(CharSequence, CharSequence) performs no joining (it just returns
  the 2nd parameter).
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringJoin")` to the enclosing element.
