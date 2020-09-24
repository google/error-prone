---
title: ParameterPackage
summary: Method parameter has wrong package
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Method does not override method in superclass due to wrong package for
parameter. For example, defining a method void foo(alpha.Foo x) when the
superclass contains a method void foo(beta.Foo x). The defined method was
probably meant to override the superclass method.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ParameterPackage")` to the enclosing element.
