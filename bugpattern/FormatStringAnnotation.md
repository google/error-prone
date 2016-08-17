---
title: FormatStringAnnotation
summary: Invalid format string passed to formatting method.
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
To require validation of method parameters that will be used as printf style
format strings, annotate the parameters with `@FormatString` and their arguments
with `@FormatArg`. The accompanying Error Prone check will enforce that
parameters passed in as format strings must be one of:

* Another @FormatString variable
* A compile time constant string
* A final or effectively final variable assigned to a compile time constant
  string
* A string literal

It will then check that the format string is guaranteed to be valid assuming it
is passed the given `@FormatArg` parameters as format arguments. For more
information on possible format string errors, see
http://errorprone.info/bugpattern/FormatString

## Suppression
Suppress false positives by adding an `@SuppressWarnings("FormatStringAnnotation")` annotation to the enclosing element.
