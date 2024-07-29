---
title: DoNotUseRuleChain
summary: Prefer using `@Rule` with an explicit order over declaring a `RuleChain`.
  RuleChain was the only way to declare ordered rules before JUnit 4.13. Newer versions
  should use the cleaner individual `@Rule(order = n)` option. The rules with a higher
  value are inner.
layout: bugpattern
tags: Refactoring
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DoNotUseRuleChain")` to the enclosing element.
