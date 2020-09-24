---
title: SelfEquals
summary: Testing an object for equality with itself will always be true.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The arguments to equals method are the same object, so it always returns true.
Either change the arguments to point to different objects or substitute true.

For test cases, instead of explicitly testing equals, use
[EqualsTester from Guava](http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SelfEquals")` to the enclosing element.
