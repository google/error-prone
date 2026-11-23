---
title: BinderIdentityRestoredDangerously
summary: A call to Binder.clearCallingIdentity() should be followed by Binder.restoreCallingIdentity()
  in a finally block. Otherwise the wrong Binder identity may be used by subsequent
  code.
layout: bugpattern
tags: FragileCode
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Binder is Android's inter-process communication mechanism. Each call to
`Binder.clearCallingIdentity()` should be followed by
`Binder.restoreCallingIdentity()` in a finally block. Otherwise the wrong Binder
identity may be used by subsequent code.

For example: `java long token = Binder.clearCallingIdentity(); // Issue a Binder
call (may throw an Exception). someBinderInterface.makeCall();
Binder.restoreCallingIdentity(token);`

The above code should be rewritten as: `java long token =
Binder.clearCallingIdentity(); try { // Issue a Binder call (may throw an
Exception). someBinderInterface.makeCall(); } finally {
Binder.restoreCallingIdentity(token); }`

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BinderIdentityRestoredDangerously")` to the enclosing element.
