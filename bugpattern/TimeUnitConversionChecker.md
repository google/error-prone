---
title: TimeUnitConversionChecker
summary: 'This TimeUnit conversion looks buggy: converting from a smaller unit to
  a larger unit (and passing a constant), converting to/from the same TimeUnit, or
  converting TimeUnits where the result is statically known to be 0 or 1 are all buggy
  patterns.'
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
This checker flags potential problems with TimeUnit conversions: 1) conversions that are statically known to be equal to 0 or 1; 2) conversions that are converting from a given unit back to the same unit; 3) conversions that are converting from a smaller unit to a larger unit and passing a constant value

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TimeUnitConversionChecker")` to the enclosing element.

