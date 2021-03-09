---
title: ProtoBuilderReturnValueIgnored
summary: 'Unnecessary call to proto''s #build() method.  If you don''t consume the
  return value of #build(), the result is discarded and the only effect is to verify
  that all required fields are set, which can be expressed more directly with #isInitialized().'
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->



## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ProtoBuilderReturnValueIgnored")` to the enclosing element.
