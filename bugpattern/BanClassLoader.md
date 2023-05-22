---
title: BanClassLoader
summary: Using dangerous ClassLoader APIs may deserialize untrusted user input into
  bytecode, leading to remote code execution vulnerabilities
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
The Java class loading APIs can lead to remote code execution vulnerabilities if
not used carefully. Interpreting potentially untrusted input as bytecode can
give an attacker control of the application.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BanClassLoader")` to the enclosing element.
