---
title: WrongOneof
summary: This field is guaranteed not to be set given it's within a switch over a
  one_of.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("WrongOneof")` to the enclosing element.
