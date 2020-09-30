---
title: BadImport
summary: Importing nested classes/static methods/static fields with commonly-used
  names can make code harder to read, because it may not be clear from the context
  exactly which type is being referred to. Qualifying the name with that of the containing
  class can make the code clearer.
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("BadImport")` to the enclosing element.
