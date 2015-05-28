---
title: NarrowingCompoundAssignment
summary: "Compound assignments to bytes, shorts, chars, and floats hide dangerous casts"
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The compound assignment E1 op= E2 could be mistaken for being equivalent to  E1 = E1 op E2. However, this is not the case: compound  assignment operators automatically cast the result of the computation to the type on the left hand side. So E1 op= E2 is actually equivalent to E1 = (T) (E1 op E2), where T is the type of E1. If the type of the expression is wider than the type of the variable (i.e. the variable is a byte, char, short, or float), then the compound assignment will perform a narrowing primitive conversion. Attempting to perform the equivalent simple assignment would generate a compilation error.

 For example, 'byte b = 0; b = b << 1;' does not compile, but 'byte b = 0; b <<= 1;' does!

 (See Puzzle #9 in 'Java Puzzlers: Traps, Pitfalls, and Corner Cases'.)

## Suppression
Suppress false positives by adding an `@SuppressWarnings("NarrowingCompoundAssignment")` annotation to the enclosing element.
