---
title: FormatStringShouldUsePlaceholders
summary: Using a format string avoids string concatenation in the common case.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
It usually hurts performance to eagerly generate error messages with +, as you pay the cost of the string conversion whether or not the condition fails. It's usually more efficient to use %s as a placeholder and to pass the additional variables as further arguments.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("FormatStringShouldUsePlaceholders")` to the enclosing element.
