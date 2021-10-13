---
title: UnnecessaryFinal
summary: Since Java 8, it's been unnecessary to make local variables and parameters
  `final` for use in lambdas or anonymous classes. Marking them as `final` is weakly
  discouraged, as it adds a fair amount of noise for minimal benefit.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("UnnecessaryFinal")` to the enclosing element.
