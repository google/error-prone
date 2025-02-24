---
title: YodaCondition
summary: The non-constant portion of a comparison generally comes first. For equality,
  prefer e.equals(CONSTANT) if e is non-null or Objects.equals(e, CONSTANT) if e may
  be null. For standard operators, prefer e <OPERATION> CONSTANT.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("YodaCondition")` to the enclosing element.
