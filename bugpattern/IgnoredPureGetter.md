---
title: IgnoredPureGetter
summary: Getters on AutoValue classes and protos are side-effect free, so there is no point in calling them if the return value is ignored.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("IgnoredPureGetter")` to the enclosing element.
