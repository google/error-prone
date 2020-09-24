---
title: StringBuilderInitWithChar
summary: StringBuilder does not have a char constructor; this invokes the int constructor.
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
StringBuilder does not have a char constructor, so instead this code creates a
StringBuilder with initial size equal to the code point of the specified char.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("StringBuilderInitWithChar")` to the enclosing element.
