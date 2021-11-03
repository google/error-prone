---
title: BareDotMetacharacter
summary: '"." is rarely useful as a regex, as it matches any character. To match a
  literal ''.'' character, instead write "\\.".'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: InvalidPatternSyntax_


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BareDotMetacharacter")` to the enclosing element.
