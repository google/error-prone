---
title: FutureTransformAsync
summary: Use transform instead of transformAsync when all returns are an immediate
  future.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The usage of `transformAsync`, `callAsync` and `submitAsync` is not necessary
when all the return values of the transformation function are immediate futures.
In this case, the usage of `transform`, `call` and `submit` is preferred.

Note that `transform` cannot be used if the body of the transformation function
throws checked exceptions.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FutureTransformAsync")` to the enclosing element.
