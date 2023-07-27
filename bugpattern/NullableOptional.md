---
title: NullableOptional
summary: Using an Optional variable which is expected to possibly be null is discouraged.
  It is best to indicate the absence of the value by assigning it an empty optional.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`Optional` is a container object which may or may not contain a value. The
presence or absence of the contained value should be demonstrated by the
`Optional` object itself.

Using an Optional variable which is expected to possibly be null is discouraged.
An nullable Optional which uses `null` to indicate the absence of the value will
lead to extra work for `null` checking when using the object and even cause
exceptions such as `NullPointerException`. It is best to indicate the absence of
the value by assigning it an empty optional.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("NullableOptional")` to the enclosing element.
