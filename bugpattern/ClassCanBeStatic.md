---
title: ClassCanBeStatic
summary: Inner class is non-static but does not reference enclosing class
layout: bugpattern
tags: Style, Performance
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
An inner class should be static unless it references members of its enclosing
class. An inner class that is made non-static unnecessarily uses more memory and
does not make the intent of the class clear.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ClassCanBeStatic")` to the enclosing element.
