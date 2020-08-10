---
title: TransientMisuse
summary: Static fields are implicitly transient, so the explicit modifier is unnecessary
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TransientMisuse")` to the enclosing element.
