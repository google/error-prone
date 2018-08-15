---
title: ProtosAsKeyOfSetOrMap
summary: Protos should not be used as a key to a map, in a set, or in a contains method on a descendant of a collection. Protos have non deterministic ordering and proto equality is deep, which is a performance issue.
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtosAsKeyOfSetOrMap")` to the enclosing element.
