---
title: StringCaseLocaleUsage
summary: 'Specify a `Locale` when calling `String#to{Lower,Upper}Case`. (Note: there
  are multiple suggested fixes; the third may be most appropriate if you''re dealing
  with ASCII Strings.)'
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringCaseLocaleUsage")` to the enclosing element.
