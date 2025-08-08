---
title: ThreadBuilderNameWithPlaceholder
summary: Thread.Builder.name() does not accept placeholders (e.g., %d or %s). threadBuilder.name(String)
  accepts a constant name and threadBuilder.name(String, int) accepts a constant name
  _prefix_ and an initial counter value.
layout: bugpattern
tags: LikelyError
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThreadBuilderNameWithPlaceholder")` to the enclosing element.
