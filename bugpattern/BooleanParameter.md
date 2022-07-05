---
title: BooleanParameter
summary: Use parameter comments to document ambiguous literals
layout: bugpattern
tags: ''
severity: SUGGESTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Providing parameter comments for boolean literals has some advantages:

*   Readability is generally improved, as the parameter name will likely provide
    some context on what the boolean literal means
*   [https://errorprone.info/bugpattern/ParameterName](ParameterName) checks at compile-time that the
    comments match the formal argument names to avoid accidentally transposing
    parameters

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BooleanParameter")` to the enclosing element.
