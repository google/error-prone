---
title: ProtoRedundantSet
summary: A field on a protocol buffer was set twice in the same chained expression.
layout: bugpattern
tags: FragileCode
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtoRedundantSet")` to the enclosing element.
