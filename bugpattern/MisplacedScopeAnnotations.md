---
title: MisplacedScopeAnnotations
summary: Scope annotations used as qualifier annotations don't have any effect. Move
  the scope annotation to the binding location or delete it.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("MisplacedScopeAnnotations")` to the enclosing element.
