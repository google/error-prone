---
title: UseTimeInScope
summary: There is already a Clock in scope here. Prefer to reuse it rather than creating a new one.  Having multiple unsynchronized time sources in scope risks accidents.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UseTimeInScope")` to the enclosing element.
