---
title: FragmentNotInstantiable
summary: 'Subclasses of Fragment must be instantiable via Class#newInstance(): the
  class must be public, static and have a public nullary constructor'
layout: bugpattern
tags: LikelyError
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ValidFragment_

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FragmentNotInstantiable")` to the enclosing element.
