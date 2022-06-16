---
title: NoCanIgnoreReturnValueOnClasses
summary: '@CanIgnoreReturnValue should not be applied to classes as it almost always
  overmatches (as it applies to constructors and all methods), and the CIRVness isn''t
  conferred to its subclasses.'
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NoCanIgnoreReturnValueOnClasses")` to the enclosing element.
