---
title: FormatStringAnnotation
summary: Invalid format string passed to formatting method.
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Methods can be annotated with Error Prone's `@FormatMethod` annotation to
indicate that calls to this function should be treated similarly to
`String.format`: One of the parameters is a 'format string' (the first String
parameter or the only parameter annotated with `@FormatString`), and the
subsequent parameters are used as format arguments to that format string.

For example:

```java
@FormatMethod
void myLogMethod(@FormatString String fmt, Object... args) {}

// ERROR: 2nd format argument isn't a number
myLogMessage("My log message: %d and %d", 3, "has a message");
```

In order to avoid complex runtime issues when the format string part is
dynamically constructed, leading to a mismatch between the arguments and format
strings, we require that the 'format string' argument in calls to
`@FormatMethod`-annotated methods be one of:

*   Another `@FormatString`-annotated variable
*   A compile time constant string
*   A final or effectively final variable assigned to a compile time constant
    string
*   A string literal

We will then check that the format string and format arguments match.

For more information on possible format string errors, see the documentation on
the [FormatString check](FormatString).

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FormatStringAnnotation")` to the enclosing element.
