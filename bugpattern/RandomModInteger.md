---
title: RandomModInteger
summary: Use Random.nextInt(int).  Random.nextInt() % n can have negative results
layout: bugpattern
category: JDK
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
`Random.nextInt() % n` has 

* a 1/n chance of being 0
* a 1/2n chance of being each number from `1` to `n-1` inclusive
* a 1/2n chance of being each number from `-1` to `-(n-1)` inclusive

Many users expect a uniformly distributed random integer between `0` and `n-1` inclusive, but you must use random.nextInt(n) to get that behavior.  If the original behavior is truly desired, use `(random.nextBoolean() ? 1 : -1) * random.nextInt(n)`.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("RandomModInteger")` annotation to the enclosing element.
