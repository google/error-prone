---
title: ArraysAsListPrimitiveArray
summary: Arrays.asList does not autobox primitive arrays, as one might expect.
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Arrays.asList does not autobox primitive arrays, as one might expect. If you intended to autobox the primitive array, use an asList method from Guava that does autobox.  If you intended to create a singleton list containing the primitive array, use Collections.singletonList to make your intent clearer.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ArraysAsListPrimitiveArray")` annotation to the enclosing element.
