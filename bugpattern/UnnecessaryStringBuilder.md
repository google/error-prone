---
title: UnnecessaryStringBuilder
summary: Prefer string concatenation over explicitly using `StringBuilder#append`,
  since `+` reads better and has equivalent or better performance.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryStringBuilder")` to the enclosing element.
