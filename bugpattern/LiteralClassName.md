---
title: LiteralClassName
summary: Using Class.forName is unnecessary if the class is available at compile-time.
layout: bugpattern
category: JDK
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("LiteralClassName")` annotation to the enclosing element.
