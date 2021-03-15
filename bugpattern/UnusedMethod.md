---
title: UnusedMethod
summary: Unused.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: Unused, unused, UnusedParameters_

## The problem
The presence of an unused method may indicate a bug. This check highlights
_private_ methods which are unused and can be safely removed without considering
the impact on other source files.

## Suppression

All false positives can be suppressed by annotating the method with
`@SuppressWarnings("unused")` or prefixing its name with `unused`.

