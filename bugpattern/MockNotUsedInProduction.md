---
title: MockNotUsedInProduction
summary: This mock is configured but never escapes to be used in production code.
  Should it be removed?
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MockNotUsedInProduction")` to the enclosing element.
