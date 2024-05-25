---
title: SunApi
summary: Usage of internal proprietary API which may be removed in a future release
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Warn on internal, proprietary APIs that may be removed in future JDK versions.

For `sun.misc.Unsafe`, note that the API will be removed from a future version
of the JDK:
[JEP 471: Deprecate the Memory-Access Methods in sun.misc.Unsafe for Removal](https://openjdk.org/jeps/471).

This check is a re-implementation of javac's 'sunapi' diagnostic.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("SunApi")` to the enclosing element.
