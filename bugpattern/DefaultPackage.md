---
title: DefaultPackage
summary: Java classes shouldn't use default package
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Declaring classes in the default package is discouraged.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DefaultPackage")` to the enclosing element.

