---
title: StreamToString
summary: Calling toString on a Stream does not provide useful information
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
The `toString` method on a `Stream` will print its identity, such as `java.util.stream.ReferencePipeline$Head@6d06d69c`. This is rarely what was intended.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("StreamToString")` annotation to the enclosing element.
