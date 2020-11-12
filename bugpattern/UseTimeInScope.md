---
title: UseTimeInScope
summary: Prefer to reuse time sources rather than creating new ones. Having multiple
  unsynchronized time sources in scope risks accidents.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UseTimeInScope")` to the enclosing element.
