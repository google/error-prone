---
title: StackTraceElementGetClass
summary: Calling getClass on StackTraceElement returns the Class object for StackTraceElement,
  you probably meant to retrieve the class containing the execution point represented
  by this stack trace element.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`StackTraceElementGetClass#getClass` returns the Class object for
`StackTraceElement`. In almost all the scenarios this is not intended and is a
potential source of bugs. The most common usage of this method is to retrieve
the name of the class where exception occurred, in such cases
`StackTraceElement#getClassName` can be used instead. In case Class object for
`StackTraceElement` is required it can be obtained using
`StackTraceElement#class` method.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StackTraceElementGetClass")` to the enclosing element.
