---
title: Finally
summary: If you return or throw from a finally, then values returned or thrown from the try-catch block will be ignored
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: finally, ThrowFromFinallyBlock_

## The problem
Terminating a finally block abruptly preempts the outcome of the try block, and will cause the result of any previously executed return or throw statements to be ignored. This is very confusing. Please refactor this code to ensure that the finally block will always complete normally.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("Finally")` annotation to the enclosing element.
