---
title: Interruption
summary: Always pass 'false' to 'Future.cancel()', unless you are propagating a cancellation-with-interrupt
  from another caller
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("Interruption")` to the enclosing element.
