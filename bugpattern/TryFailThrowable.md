---
title: TryFailThrowable
summary: Catching Throwable/Error masks failures from fail() or assert*() in the try
  block
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
When testing that a line of code throws an expected exception, it is typical to
execute that line in a try block with a `fail()` or `assert*()` on the line
following. The expectation is that the expected exception will be thrown, and
execution will continue in the catch block, and the `fail()` or `assert*()` will
not be executed.

`fail()` and `assert*()` throw AssertionErrors, which are a subtype of
Throwable. That means that if the catch block catches Throwable, then execution
will always jump to the catch block, and the test will always pass.

To fix this, you usually want to catch Exception rather than Throwable. If you
need to catch throwable (e.g., the expected exception is an AssertionError),
then add logic in your catch block to ensure that the AssertionError that was
caught is not the same one thrown by the call to `fail()` or `assert*()`.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("TryFailThrowable")` to the enclosing element.
