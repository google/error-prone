---
title: MockNotUsedInProduction
summary: This mock is instantiated and configured, but is never passed to production
  code. It should be either removed or used.
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
