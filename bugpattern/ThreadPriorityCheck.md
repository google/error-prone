---
title: ThreadPriorityCheck
summary: Relying on the thread scheduler is discouraged.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Don't rely on the thread scheduler for correctness or performance. Instead,
ensure that the average number of runnable threads is not significantly greater
than the number of processors, i.e. by using the executor framework and an
appropriately sized thread pool.

For more information, see [Effective Java 3rd Edition §84][ej3e-84].

[ej3e-84]: https://books.google.com/books?id=BIpDDwAAQBAJ

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ThreadPriorityCheck")` to the enclosing element.
