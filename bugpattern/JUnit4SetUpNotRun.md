---
title: JUnit4SetUpNotRun
summary: setUp() method will not be run; please add JUnit's @Before annotation
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
JUnit 3 provides the method setUp(), to be overridden by subclasses when the
test needs to perform some pre-test initialization. In JUnit 4, this is
accomplished by annotating such a method with @Before.

The method that triggered this error matches the definition of setUp() from
JUnit3, but was not annotated with @Before and thus won't be run by the JUnit4
runner.

If you intend for this setUp() method not to run by the JUnit4 runner, but
perhaps manually be invoked in certain test methods, please rename the method or
mark it private.

If the method is part of an abstract test class hierarchy where this class's
setUp() is invoked by a superclass method that is annotated with @Before, then
please rename the abstract method or add @Before to the superclass's definition
of setUp()

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("JUnit4SetUpNotRun")` to the enclosing element.
