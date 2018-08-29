---
title: Unused
summary: Unused.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: unused, UnusedParameters_

## The problem
The presence of an unused declaration may indicate a bug.

## Suppression

Suppress false positives by adding the suppression annotation
`@SuppressWarnings("unused")` to the enclosing element.

