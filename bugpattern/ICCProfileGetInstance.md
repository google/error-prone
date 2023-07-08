---
title: ICCProfileGetInstance
summary: This method searches the class path for the given file, prefer to read the
  file and call getInstance(byte[]) or getInstance(InputStream)
layout: bugpattern
tags: Performance
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
`ICC_Profile.getInstance(String)` searches the entire classpath, which is often
unnecessary and can result in slow performance for applications with long
classpaths. Prefer `getInstance(byte[])` or `getInstance(InputStream)` instead.

See also https://bugs.openjdk.org/browse/JDK-8191622.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ICCProfileGetInstance")` to the enclosing element.
