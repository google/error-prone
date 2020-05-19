---
title: JavaLocalTimeGetNano
summary: localTime.getNano() only accesses the nanos-of-second field. It's rare to only use getNano() without a nearby getSecond() call.
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JavaLocalTimeGetNano")` to the enclosing element.

