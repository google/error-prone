---
title: IgnoredPureGetter
summary: Getters on AutoValue classes and protos are side-effect free, so there is
  no point in calling them if the return value is ignored. While there are no side
  effects from the getter, the receiver may have side effects.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IgnoredPureGetter")` to the enclosing element.
