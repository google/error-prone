---
title: RxReturnValueIgnored
summary: Returned Rx objects must be checked. Ignoring a returned Rx value means it is never scheduled for execution
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Methods that return an ignored [Observable | Single | Flowable | Maybe ] generally indicate errors.

If you don’t check the return value of these methods, the observables may never execute. It also means the error case is not being handled

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("RxReturnValueIgnored")` to the enclosing element.
