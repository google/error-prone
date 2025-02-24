---
title: SelfAssertion
summary: This assertion will always fail or succeed.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: TruthSelfEquals_

## The problem
If a test subject and the argument to `isEqualTo` are the same instance (e.g.
`assertThat(x).isEqualTo(x)`), then the assertion will always pass. Truth
implements `isEqualTo` using [`Objects#equal`] , which tests its arguments for
reference equality and returns true without calling `equals()` if both arguments
are the same instance.

JUnit's `assertEquals` (and similar) methods are implemented in terms of
`Object#equals`. However, this is not explicitly documented, so isn't a
contractual guarantee of the assertion methods.

[`Objects#equals`]: https://guava.dev/releases/21.0/api/docs/com/google/common/base/Objects.html#equal-java.lang.Object-java.lang.Object-

To test the implementation of an `equals` method, use
[Guava's EqualsTester][javadoc], or explicitly call `equals` as part of the
test.

[javadoc]: http://static.javadoc.io/com.google.guava/guava-testlib/21.0/com/google/common/testing/EqualsTester.html

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SelfAssertion")` to the enclosing element.
