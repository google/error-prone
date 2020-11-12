---
title: OptionalNotPresent
summary: One should not call optional.get() inside an if statement that checks !optional.isPresent
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("OptionalNotPresent")` to the enclosing element.
